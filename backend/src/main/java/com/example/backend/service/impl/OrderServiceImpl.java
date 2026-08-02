package com.example.backend.service.impl;

import com.example.backend.dto.request.CreateOrderRequest;
import com.example.backend.dto.response.OrderHistoryDTO;
import com.example.backend.dto.response.OrderItemResponse;
import com.example.backend.dto.response.OrderResponse;
import com.example.backend.dto.response.VoucherValidationResponse;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import com.example.backend.service.CartService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.OrderService;
import com.example.backend.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final VoucherService voucherService;
    private final VoucherRepository voucherRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng của bạn đang trống!"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống, không thể đặt hàng!");
        }

        for (CartItem ci : cartItems) {
            ProductVariant variant = ci.getVariant();
            if (variant.getStockQuantity() < ci.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + variant.getProduct().getName() +
                        " (" + variant.getSize() + "-" + variant.getColor() + ")" +
                        " chỉ còn " + variant.getStockQuantity() + " SP, không đủ để đặt " + ci.getQuantity() + " SP!");
            }
        }

        long count = orderRepository.count() + 1;
        String orderCode = String.format("DH%03d", count);
        while (orderRepository.existsByOrderCode(orderCode)) {
            count++;
            orderCode = String.format("DH%03d", count);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .recipientName(request.getReceiverName() != null ? request.getReceiverName() : user.getFullName())
                .recipientPhone(request.getReceiverPhone() != null ? request.getReceiverPhone() : user.getPhone())
                .shippingAddress(request.getShippingAddress() != null ? request.getShippingAddress() : user.getAddress())
                .note(request.getNote())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "COD")
                .paymentStatus("UNPAID")
                .orderStatus("PENDING")
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .histories(new ArrayList<>())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (CartItem ci : cartItems) {
            ProductVariant variant = ci.getVariant();
            Product product = variant.getProduct();

            BigDecimal price = (product.getSalePrice() != null && product.getSalePrice().compareTo(BigDecimal.ZERO) > 0)
                    ? product.getSalePrice()
                    : product.getPrice();

            BigDecimal subTotal = price.multiply(BigDecimal.valueOf(ci.getQuantity()));
            totalAmount = totalAmount.add(subTotal);

            variant.setStockQuantity(variant.getStockQuantity() - ci.getQuantity());
            productVariantRepository.save(variant);

            if (variant.getStockQuantity() <= 0) {
                notificationService.notifyCartUsersIfVariantOutOfStock(variant.getId());
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .variant(variant)
                    .productName(product.getName())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .price(price)
                    .quantity(ci.getQuantity())
                    .build();

            orderItemRepository.save(orderItem);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalTotalAmount = totalAmount;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            VoucherValidationResponse voucherResult = voucherService.validateVoucher(request.getVoucherCode(), totalAmount);
            if (!voucherResult.isValid()) {
                throw new RuntimeException(voucherResult.getMessage());
            }
            discountAmount = voucherResult.getDiscountAmount();
            finalTotalAmount = voucherResult.getFinalAmount();
            savedOrder.setVoucherCode(voucherResult.getCode());
            savedOrder.setDiscountAmount(discountAmount);

            voucherRepository.findByCodeIgnoreCase(voucherResult.getCode()).ifPresent(v -> {
                int newUsed = (v.getUsedCount() != null ? v.getUsedCount() : 0) + 1;
                v.setUsedCount(newUsed);
                voucherRepository.save(v);

                if (v.getUsageLimit() != null && newUsed >= v.getUsageLimit() && Boolean.TRUE.equals(v.getIsPublic())) {
                    notificationService.createNotification(
                            null,
                            "⚠️ Voucher Đã Hết Lượt!",
                            "Mã giảm giá " + v.getCode() + " (" + v.getName() + ") đã được sử dụng hết số lượt quy định.",
                            "VOUCHER_DEPLETED",
                            null
                    );
                }
            });
        }

        savedOrder.setTotalAmount(finalTotalAmount);
    
        OrderHistory initialHistory = OrderHistory.builder()
                .order(savedOrder)
                .status("PENDING")
                .note("Đơn hàng đã được tạo thành công và đang chờ cửa hàng xác nhận.")
                .build();

        orderHistoryRepository.save(initialHistory);
        savedOrder = orderRepository.save(savedOrder);

        cartService.clearCart(userId);

        if (savedOrder.getUser() != null) {
            notificationService.createNotification(
                    savedOrder.getUser().getId(),
                    "🛒 Đặt hàng thành công! Mã đơn: " + savedOrder.getOrderCode(),
                    "Đơn hàng của bạn đã được khởi tạo thành công và đang chờ cửa hàng xác nhận duyệt.",
                    "ORDER_CREATED",
                    "orders"
            );
        }

        return mapToOrderResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderDetail(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));
        return mapToOrderResponse(order);
    }

    // Customer Cancel Logic:
    // Before Admin approval (PENDING): Cancel immediately & restore stock.
    // After Admin approval (CONFIRMED/SHIPPING/DELIVERED): Create cancellation request (CANCEL_REQUESTED) awaiting Admin review.
    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        String currentStatus = order.getOrderStatus();

        if ("PENDING".equalsIgnoreCase(currentStatus)) {
            // Free cancel before Admin approval
            order.setOrderStatus("CANCELLED");
            order.setPaymentStatus("CANCELLED");

            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                    productVariantRepository.save(variant);
                }
            }

            OrderHistory cancelHistory = OrderHistory.builder()
                    .order(order)
                    .status("CANCELLED")
                    .note("Khách hàng đã hủy đơn hàng (trước khi cửa hàng duyệt). Lý do: " + (reason != null ? reason : "Thay đổi ý định mua hàng."))
                    .build();

            orderHistoryRepository.save(cancelHistory);
        } else if ("CONFIRMED".equalsIgnoreCase(currentStatus) || "SHIPPING".equalsIgnoreCase(currentStatus) || "DELIVERED".equalsIgnoreCase(currentStatus)) {
            // After Admin approval: Request cancellation requiring Admin review
            order.setOrderStatus("CANCEL_REQUESTED");

            OrderHistory requestHistory = OrderHistory.builder()
                    .order(order)
                    .status("CANCEL_REQUESTED")
                    .note("Khách hàng đã gửi Yêu Cầu Hủy Đơn. Lý do: " + (reason != null ? reason : "Thay đổi ý định.") + " (Đang chờ Admin duyệt)")
                    .build();

            orderHistoryRepository.save(requestHistory);
        } else {
            throw new RuntimeException("Đơn hàng không ở trạng thái cho phép gửi yêu cầu hủy!");
        }

        Order updatedOrder = orderRepository.save(order);

        if (updatedOrder.getUser() != null) {
            if ("CANCELLED".equalsIgnoreCase(updatedOrder.getOrderStatus())) {
                notificationService.createNotification(
                        updatedOrder.getUser().getId(),
                        "❌ Đã hủy đơn hàng " + updatedOrder.getOrderCode(),
                        "Bạn đã hủy thành công đơn hàng " + updatedOrder.getOrderCode() + ". Tồn kho sản phẩm đã được hoàn trả.",
                        "ORDER_CANCELLED",
                        "orders"
                );
            } else if ("CANCEL_REQUESTED".equalsIgnoreCase(updatedOrder.getOrderStatus())) {
                notificationService.createNotification(
                        updatedOrder.getUser().getId(),
                        "⏳ Đã gửi yêu cầu hủy đơn " + updatedOrder.getOrderCode(),
                        "Yêu cầu hủy đơn hàng của bạn đã được gửi tới cửa hàng và đang chờ Quản trị viên xét duyệt.",
                        "ORDER_CANCEL_REQUESTED",
                        "orders"
                );
            }
        }

        return mapToOrderResponse(updatedOrder);
    }

    // Customer 2-Way Delivery Confirmation (Only allowed after Admin confirmed delivery - DELIVERED status!)
    @Override
    @Transactional
    public OrderResponse confirmOrderReceived(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        if (!"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Vui lòng chờ Admin xác nhận đã giao hàng thành công trước khi bạn xác nhận hoàn tất đơn hàng 2 phía!");
        }

        order.setOrderStatus("COMPLETED");
        order.setPaymentStatus("PAID");

        OrderHistory history = OrderHistory.builder()
                .order(order)
                .status("COMPLETED")
                .note("Khách hàng đã xác nhận đã nhận được hàng. Đơn hàng chính thức HOÀN TẤT 2 PHÍA!")
                .build();

        orderHistoryRepository.save(history);
        Order updated = orderRepository.save(order);

        if (updated.getUser() != null) {
            notificationService.createNotification(
                    updated.getUser().getId(),
                    "🎉 Đơn hàng " + updated.getOrderCode() + " đã hoàn tất!",
                    "Cảm ơn bạn đã mua hàng tại FS SHOP! Đừng quên viết đánh giá sản phẩm nhé.",
                    "ORDER_COMPLETED",
                    "orders"
            );
        }

        return mapToOrderResponse(updated);
    }

    // Admin: Get all orders across system
    @Override
    public List<OrderResponse> getAllOrdersAdmin(String status) {
        List<Order> orders;
        if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status.trim())) {
            orders = orderRepository.findAllByOrderByIdDesc();
        } else {
            orders = orderRepository.findByOrderStatusOrderByIdDesc(status.trim().toUpperCase());
        }

        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    // Admin: Update Order Status Progression
    @Override
    @Transactional
    public OrderResponse updateOrderStatusAdmin(Long orderId, String newStatus, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        String currentStatus = order.getOrderStatus();
        String formattedStatus = newStatus.trim().toUpperCase();

        if ("COMPLETED".equalsIgnoreCase(formattedStatus)) {
            throw new RuntimeException("Admin không thể tự chuyển đơn sang Hoàn Thành! Đơn hàng bắt buộc cần Khách Hàng bấm nút 'Xác nhận đã nhận hàng' để hoàn tất 2 phía.");
        }

        // Strict progression check (unless cancelling)
        if ("CANCELLED".equalsIgnoreCase(formattedStatus)) {
            if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
                throw new RuntimeException("Đơn hàng đã hoàn tất 2 phía, không thể hủy!");
            }
            order.setOrderStatus("CANCELLED");
            order.setPaymentStatus("CANCELLED");

            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                    productVariantRepository.save(variant);
                }
            }
        } else if ("PENDING".equalsIgnoreCase(currentStatus) && "CONFIRMED".equalsIgnoreCase(formattedStatus)) {
            order.setOrderStatus("CONFIRMED");
        } else if ("CONFIRMED".equalsIgnoreCase(currentStatus) && "SHIPPING".equalsIgnoreCase(formattedStatus)) {
            order.setOrderStatus("SHIPPING");
        } else if ("SHIPPING".equalsIgnoreCase(currentStatus) && "DELIVERED".equalsIgnoreCase(formattedStatus)) {
            order.setOrderStatus("DELIVERED");
        } else {
            throw new RuntimeException("Chuyển trạng thái không hợp lệ! Vui lòng tuân thủ quy trình: Chờ duyệt ➔ Đã duyệt ➔ Đang giao ➔ Báo đã giao (Chờ khách xác nhận).");
        }

        String defaultNote;
        if ("DELIVERED".equalsIgnoreCase(formattedStatus)) {
            defaultNote = "Admin xác nhận đã giao hàng thành công (Xác nhận 1 phía từ Admin). Đang chờ khách hàng xác nhận nhận hàng.";
        } else if ("CANCELLED".equalsIgnoreCase(formattedStatus)) {
            defaultNote = "Admin đã đơn phương hủy đơn hàng. Tồn kho sản phẩm đã được hoàn trả.";
        } else {
            defaultNote = "Quản trị viên đã cập nhật trạng thái đơn hàng thành: " + formattedStatus;
        }

        String historyNote = (note != null && !note.trim().isEmpty()) ? note : defaultNote;

        OrderHistory history = OrderHistory.builder()
                .order(order)
                .status(formattedStatus)
                .note(historyNote)
                .build();

        orderHistoryRepository.save(history);
        Order updated = orderRepository.save(order);

        if (updated.getUser() != null) {
            String title;
            String notifType;
            switch (formattedStatus) {
                case "CONFIRMED":
                    title = "✅ Đơn hàng " + updated.getOrderCode() + " đã được xác nhận!";
                    notifType = "ORDER_CONFIRMED";
                    break;
                case "SHIPPING":
                    title = "🚚 Đơn hàng " + updated.getOrderCode() + " đang trên đường giao!";
                    notifType = "ORDER_SHIPPING";
                    break;
                case "DELIVERED":
                    title = "📦 Đơn hàng " + updated.getOrderCode() + " đã được giao đến!";
                    notifType = "ORDER_DELIVERED";
                    break;
                case "CANCELLED":
                    title = "❌ Đơn hàng " + updated.getOrderCode() + " đã bị hủy!";
                    notifType = "ORDER_CANCELLED";
                    break;
                default:
                    title = "ℹ️ Đơn hàng " + updated.getOrderCode() + " cập nhật trạng thái mới";
                    notifType = "ORDER_UPDATE";
            }

            notificationService.createNotification(
                    updated.getUser().getId(),
                    title,
                    historyNote,
                    notifType,
                    "orders"
            );
        }

        return mapToOrderResponse(updated);
    }

    // Admin Review Customer Cancellation Request (Duyệt hoặc Từ Chối Yêu Cầu Hủy Đơn)
    @Override
    @Transactional
    public OrderResponse adminApproveCancelOrder(Long orderId, boolean approve, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        if (!"CANCEL_REQUESTED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Đơn hàng này hiện không có yêu cầu hủy nào cần duyệt!");
        }

        if (approve) {
            order.setOrderStatus("CANCELLED");
            order.setPaymentStatus("CANCELLED");

            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                    productVariantRepository.save(variant);
                }
            }

            OrderHistory history = OrderHistory.builder()
                    .order(order)
                    .status("CANCELLED")
                    .note("Admin ĐÃ DUYỆT ĐỒNG Ý yêu cầu hủy đơn hàng của khách hàng. " + (note != null ? note : ""))
                    .build();

            orderHistoryRepository.save(history);
        } else {
            order.setOrderStatus("CONFIRMED");

            OrderHistory history = OrderHistory.builder()
                    .order(order)
                    .status("CONFIRMED")
                    .note("Admin ĐÃ TỪ CHỐI yêu cầu hủy đơn hàng của khách hàng. Đơn hàng tiếp tục quy trình xử lý giao hàng. " + (note != null ? note : ""))
                    .build();

            orderHistoryRepository.save(history);
        }

        Order updated = orderRepository.save(order);

        if (updated.getUser() != null) {
            if (approve) {
                notificationService.createNotification(
                        updated.getUser().getId(),
                        "❌ Yêu cầu hủy đơn hàng " + updated.getOrderCode() + " đã được chấp nhận",
                        "Quản trị viên đã duyệt đồng ý hủy đơn hàng theo yêu cầu của bạn. Tồn kho đã hoàn lại.",
                        "ORDER_CANCELLED",
                        "orders"
                );
            } else {
                notificationService.createNotification(
                        updated.getUser().getId(),
                        "ℹ️ Yêu cầu hủy đơn hàng " + updated.getOrderCode() + " bị từ chối",
                        "Quản trị viên đã từ chối yêu cầu hủy đơn hàng. Đơn hàng của bạn tiếp tục được xử lý giao tới bạn.",
                        "ORDER_CONFIRMED",
                        "orders"
                );
            }
        }

        return mapToOrderResponse(updated);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream().map(item -> {
            ProductVariant variant = item.getVariant();
            Product product = item.getProduct() != null ? item.getProduct() : ((variant != null) ? variant.getProduct() : null);

            if (product == null && item.getProductName() != null && !item.getProductName().trim().isEmpty()) {
                try {
                    List<Product> matches = productRepository.searchByNameOrSku(item.getProductName().trim());
                    if (matches != null && !matches.isEmpty()) {
                        product = matches.get(0);
                    }
                } catch (Exception ignored) {
                }
            }

            String productName = item.getProductName() != null ? item.getProductName() : (product != null ? product.getName() : "");
            String productSku = (product != null) ? product.getSku() : "";
            String thumbnailUrl = (product != null && product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0).getImageUrl()
                    : null;

            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(product != null ? product.getId() : null)
                    .productName(productName)
                    .productSku(productSku)
                    .thumbnailUrl(thumbnailUrl)
                    .variantId(variant != null ? variant.getId() : null)
                    .size(item.getSize())
                    .color(item.getColor())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .subTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();
        }).collect(Collectors.toList());

        List<OrderHistory> histories = orderHistoryRepository.findByOrderIdOrderByIdAsc(order.getId());
        List<OrderHistoryDTO> historyDTOs = histories.stream().map(h -> OrderHistoryDTO.builder()
                .id(h.getId())
                .status(h.getStatus())
                .description(h.getNote())
                .createdAt(h.getCreatedAt())
                .build()).collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser().getId())
                .receiverName(order.getRecipientName())
                .receiverPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .voucherCode(order.getVoucherCode())
                .discountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO)
                .items(itemResponses)
                .histories(historyDTOs)
                .createdAt(order.getCreatedAt())
                .build();
    }
}

// Feature Implementation: api create order, trừ tồn kho

// Feature Implementation: ui/api hủy đơn

// Feature Implementation: api đóng đơn, tính doanh thu
