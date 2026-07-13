import { reactive } from 'vue';

const savedUser = localStorage.getItem('user');

export const authState = reactive({
  user: savedUser ? JSON.parse(savedUser) : null,
  token: localStorage.getItem('token') || null,

  setUser(user, token) {
    this.user = user;
    this.token = token;
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
    if (token) {
      localStorage.setItem('token', token);
    } else {
      localStorage.removeItem('token');
    }
  },

  logout() {
    this.user = null;
    this.token = null;
    localStorage.removeItem('user');
    localStorage.removeItem('token');
  }
});
