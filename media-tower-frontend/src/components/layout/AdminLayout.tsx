// Fichier : src/components/layout/AdminLayout.tsx (COMPLET ET SIMPLIFIÉ)

import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, ShoppingBasket, Layers, Users, LogOut, Package, FileText, Settings as SettingsIcon, Tag, MessageSquare, Percent, Star, Box } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { auth } from '../../lib/firebase';
import NotificationDropdown from '../admin/NotificationDropdown';

const AdminLayout = ({ children }: { children: ReactNode }) => {
  const { appUser } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await auth.signOut();
    navigate('/login');
  };

  const navItems = [
    { name: 'Dashboard', icon: LayoutDashboard, path: '/admin/dashboard' },
    { name: 'Products', icon: ShoppingBasket, path: '/admin/products' },
    { name: 'Packs', icon: Box, path: '/admin/packs' },
    { name: 'Services', icon: ShoppingBasket, path: '/admin/services' },
    { name: 'Categories', icon: Layers, path: '/admin/categories' },
    { name: 'Tags', icon: Tag, path: '/admin/tags' },
    { name: 'Orders', icon: Package, path: '/admin/orders' },
    { name: 'Bookings', icon: Package, path: '/admin/bookings' },
    { name: 'Promotions', icon: Percent, path: '/admin/promotions' },
    { name: 'Inbox', icon: MessageSquare, path: '/admin/inbox' },
    { name: 'Product Reviews', icon: Star, path: '/admin/reviews' },
    { name: 'Users', icon: Users, path: '/admin/users' },
    { name: 'Content', icon: FileText, path: '/admin/content' },
    { name: 'Settings', icon: SettingsIcon, path: '/admin/settings' },
  ];

  return (
      <div className="flex h-screen bg-background text-foreground">
        <aside className="w-64 bg-card flex-shrink-0 border-r border-border overflow-y-auto">
          <div className="p-4">
            <h1 className="text-2xl font-bold text-white">Media Tower</h1>
          </div>
          <nav className="mt-6">
            {navItems.map((item) => (
                <NavLink
                    key={item.name}
                    to={item.path}
                    className={({ isActive }) => `flex items-center px-4 py-3 text-gray-300 hover:bg-secondary hover:text-white transition-colors ${isActive ? 'bg-primary text-white' : ''}`}
                >
                  <item.icon className="w-5 h-5 mr-3" />
                  {item.name}
                </NavLink>
            ))}
          </nav>
        </aside>
        <div className="flex-1 flex flex-col overflow-hidden">
          <header className="bg-card p-4 flex justify-between items-center border-b border-border">
            <div></div>
            <div className="flex items-center space-x-6">
              <NotificationDropdown />
              <div className="flex items-center space-x-2">
                <span className="text-gray-300">{appUser?.firstName || appUser?.email}</span>
                <button onClick={handleLogout} className="flex items-center text-gray-400 hover:text-red-500 transition-colors" title="Logout">
                  <LogOut className="w-5 h-5" />
                </button>
              </div>
            </div>
          </header>
          <main className="flex-1 overflow-y-auto p-6">
            {children}
          </main>
        </div>
      </div>
  );
};
export default AdminLayout;