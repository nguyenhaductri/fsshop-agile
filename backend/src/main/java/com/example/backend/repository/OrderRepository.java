package com.example.backend.repository;

import com.example.backend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByIdDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    boolean existsByOrderCode(String orderCode);

    Page<Order> findAllByOrderByIdDesc(Pageable pageable);

    List<Order> findAllByOrderByIdDesc();

    List<Order> findByOrderStatusOrderByIdDesc(String orderStatus);

    // --- Dashboard Revenue Queries ---

    // Total revenue from COMPLETED orders
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = 'COMPLETED'")
    Long sumTotalRevenue();

    // Total COMPLETED orders count
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = 'COMPLETED'")
    Long countCompletedOrders();

    // Total products sold from COMPLETED orders (sum of quantities)
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.order.orderStatus = 'COMPLETED'")
    Long sumTotalProductsSold();

    // Monthly revenue grouped by year and month for COMPLETED orders
    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
           "FROM Order o WHERE o.orderStatus = 'COMPLETED' AND YEAR(o.createdAt) = :year " +
           "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) ORDER BY MONTH(o.createdAt) ASC")
    List<Object[]> findMonthlyRevenueByYear(@Param("year") int year);

    // Top 5 best-selling products by quantity from COMPLETED orders
    @Query("SELECT oi.variant.product.id, oi.productName, COALESCE(SUM(oi.quantity), 0), COALESCE(SUM(oi.price * oi.quantity), 0) " +
           "FROM OrderItem oi WHERE oi.order.orderStatus = 'COMPLETED' " +
           "GROUP BY oi.variant.product.id, oi.productName ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopProductsBySales(Pageable pageable);

    // All order items from COMPLETED orders with date info
    @Query("SELECT oi.variant.product.id, oi.productName, oi.quantity, (oi.price * oi.quantity), oi.order.createdAt " +
           "FROM OrderItem oi WHERE oi.order.orderStatus = 'COMPLETED'")
    List<Object[]> findAllCompletedOrderItems();

    // All orders with total amount, status, created at
    @Query("SELECT o.id, o.totalAmount, o.orderStatus, o.createdAt FROM Order o")
    List<Object[]> findAllOrdersSummary();
}

