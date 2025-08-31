import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Outlet } from 'react-router-dom';
// Correction: Import AuthProvider from the new file AuthProvider, and useAuth from AuthContext.
import { useAuth } from './contexts/AuthContext';
import { AuthProvider } from './contexts/AuthProvider';
import { CartProvider } from './contexts/CartContext';
import { SettingsProvider } from './contexts/SettingsContext';
import { requestNotificationPermission } from './lib/firebase';

// Layouts
import AdminLayout from './components/layout/AdminLayout';
import SiteLayout from './components/layout/SiteLayout';
import ProtectedRoute from './components/shared/ProtectedRoute';
import AuthRedirect from './components/auth/AuthRedirect';

// Auth Pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';

// Admin Pages
import DashboardAdmin from './pages/admin/Dashboard';
import ProductsAdmin from './pages/admin/Products';
import ServicesAdmin from './pages/admin/Services';
import CategoriesAdmin from './pages/admin/Categories';
import UsersAdmin from './pages/admin/Users';
import OrdersAdmin from './pages/admin/Orders';
import BookingsAdmin from './pages/admin/Bookings';
import PromotionsAdmin from './pages/admin/Promotions';
import ContentAdmin from './pages/admin/Content';
import SettingsAdmin from './pages/admin/Settings';
import TagsAdmin from './pages/admin/Tags';
import ReviewsAdmin from './pages/admin/Reviews';
import PacksAdmin from './pages/admin/Packs';
import InboxAdmin from './pages/admin/Inbox';

// Site Pages
import HomePage from './pages/site/HomePage';
import ProductListPage from './pages/site/ProductListPage';
import ServiceListPage from './pages/site/ServiceListPage';
import ProductDetailsPage from './pages/site/ProductDetailsPage';
import ServiceDetailsPage from './pages/site/ServiceDetailsPage';
import CartPage from './pages/site/CartPage';
import CheckoutPage from './pages/site/CheckoutPage';
import OrderConfirmationPage from './pages/site/OrderConfirmationPage';
import ContentPage from './pages/site/ContentPage';
import AccountPage from './pages/site/AccountPage';

function AppContent() {
    const { currentUser } = useAuth();

    useEffect(() => {
        if (currentUser && typeof Notification !== 'undefined' && Notification.permission !== 'granted') {
            void requestNotificationPermission();
        }
    }, [currentUser]);

    return (
        <Routes>
            <Route path="/" element={<SiteLayout><Outlet/></SiteLayout>}>
                <Route index element={<HomePage />} />
                <Route path="products" element={<ProductListPage />} />
                <Route path="products/:id" element={<ProductDetailsPage />} />
                <Route path="services" element={<ServiceListPage />} />
                <Route path="services/:id" element={<ServiceDetailsPage />} />
                <Route path="cart" element={<CartPage />} />
                <Route path="checkout" element={<CheckoutPage />} />
                <Route path="order-confirmation/:orderId" element={<OrderConfirmationPage />} />
                <Route path="page/:slug" element={<ContentPage />} />
                <Route path="account" element={<ProtectedRoute allowedRoles={['USER', 'SELLER', 'ADMIN']}><AccountPage /></ProtectedRoute>} />
            </Route>

            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/auth-redirect" element={<AuthRedirect />} />

            <Route path="/admin" element={<ProtectedRoute allowedRoles={['ADMIN', 'SELLER']}><AdminLayout><Outlet /></AdminLayout></ProtectedRoute>}>
                <Route path="dashboard" element={<DashboardAdmin />} />
                <Route path="products" element={<ProductsAdmin />} />
                <Route path="packs" element={<PacksAdmin />} />
                <Route path="services" element={<ServicesAdmin />} />
                <Route path="categories" element={<CategoriesAdmin />} />
                <Route path="tags" element={<TagsAdmin />} />
                <Route path="reviews" element={<ReviewsAdmin />} />
                <Route path="orders" element={<OrdersAdmin />} />
                <Route path="bookings" element={<BookingsAdmin />} />
                <Route path="promotions" element={<PromotionsAdmin />} />
                <Route path="inbox" element={<InboxAdmin />} />
                <Route path="users" element={<UsersAdmin />} />
                <Route path="content" element={<ContentAdmin />} />
                <Route path="settings" element={<SettingsAdmin />} />
            </Route>
        </Routes>
    );
}

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <SettingsProvider>
                    <CartProvider>
                        <AppContent />
                    </CartProvider>
                </SettingsProvider>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;