// Fichier : src/pages/site/AccountPage.tsx (COMPLET ET NETTOYÉ)

import { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import { useAuth } from '../../hooks/useAuth';
// --- Imports inutilisés supprimés ---

// --- Interfaces nettoyées ---
interface OrderHistory {
    id: number;
    orderDate: string;
    status: string;
    totalAmount: number;
}
// Les interfaces pour les statistiques ont été supprimées car les graphiques ne sont plus là

const AccountPage = () => {
    const { appUser } = useAuth();
    // --- États inutilisés supprimés ---
    const [orders, setOrders] = useState<OrderHistory[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!appUser) return;

        const fetchData = async () => {
            try {
                // --- On ne récupère plus que les commandes ---
                const ordersRes = await axiosClient.get<OrderHistory[]>('/users/me/orders');
                setOrders(ordersRes.data);
            } catch (error) {
                console.error('Failed to fetch user account data:', error);
            } finally {
                setLoading(false);
            }
        };
        void fetchData();
    }, [appUser]);

    if (loading || !appUser) {
        return <div className="text-center p-8 text-gray-400">Loading user data...</div>;
    }

    return (
        <div className="space-y-12">
            <h1 className="text-4xl font-bold text-white">My Account</h1>

            <section className="bg-card border border-gray-700 rounded-lg p-6">
                <h2 className="text-2xl font-semibold mb-4 text-white">My Profile</h2>
                <div className="space-y-2 text-gray-300">
                    <p><strong>First Name:</strong> {appUser.firstName || 'Not set'}</p>
                    <p><strong>Last Name:</strong> {appUser.lastName || 'Not set'}</p>
                    <p><strong>Email:</strong> {appUser.email}</p>
                </div>
            </section>

            <section>
                <h2 className="text-2xl font-semibold mb-4 text-white">Order History</h2>
                <div className="bg-card border border-gray-700 rounded-lg p-4 overflow-x-auto">
                    <table className="w-full text-left min-w-[600px]">
                        <thead>
                        <tr className="border-b border-gray-600">
                            <th className="p-3">Order ID</th>
                            <th className="p-3">Date</th>
                            <th className="p-3">Status</th>
                            <th className="p-3 text-right">Total</th>
                        </tr>
                        </thead>
                        <tbody>
                        {orders.length > 0 ? (
                            orders.map(order => (
                                <tr key={order.id} className="border-b border-gray-700 hover:bg-gray-800">
                                    <td className="p-3 font-mono">#{order.id}</td>
                                    <td className="p-3">{new Date(order.orderDate).toLocaleDateString()}</td>
                                    <td className="p-3">{order.status}</td>
                                    <td className="p-3 text-right font-medium">{order.totalAmount.toFixed(2)} DH</td>
                                </tr>
                            ))
                        ) : (
                            <tr><td colSpan={4} className="text-center p-8 text-gray-400">You have no orders yet.</td></tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
};

export default AccountPage;