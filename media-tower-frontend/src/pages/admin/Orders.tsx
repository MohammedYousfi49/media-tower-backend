import { useEffect, useState } from 'react';
import { Eye, Clock, CheckCircle, Truck, XCircle, RefreshCcw, PackageCheck, Download } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Modal from '../../components/shared/Modal';
import { AxiosError } from 'axios';

// Interfaces pour la structure des données
interface OrderItem {
    productName: string;
    quantity: number;
    unitPrice: number;
}

interface OrderUser {
    firstName: string;
    lastName: string;
    email: string;
}

type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'REFUNDED';

interface Order {
    id: number;
    user: OrderUser;
    orderDate: string;
    status: OrderStatus;
    totalAmount: number;
    orderItems: OrderItem[];
}

const ORDER_STATUSES: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];

const StatusBadge = ({ status }: { status: OrderStatus }) => {
    const statusConfig = {
        PENDING: { icon: Clock, color: 'text-yellow-400', label: 'Pending' },
        CONFIRMED: { icon: CheckCircle, color: 'text-blue-400', label: 'Confirmed' },
        PROCESSING: { icon: RefreshCcw, color: 'text-indigo-400', label: 'Processing' },
        SHIPPED: { icon: Truck, color: 'text-purple-400', label: 'Shipped' },
        DELIVERED: { icon: PackageCheck, color: 'text-green-400', label: 'Delivered' },
        CANCELLED: { icon: XCircle, color: 'text-red-400', label: 'Cancelled' },
        REFUNDED: { icon: XCircle, color: 'text-gray-400', label: 'Refunded' },
    };
    const config = statusConfig[status] || statusConfig.PENDING;
    const Icon = config.icon;

    return (
        <span className={`flex items-center text-sm font-medium ${config.color}`}>
            <Icon className="w-4 h-4 mr-2" />
            {config.label}
        </span>
    );
};


const Orders = () => {
    const [orders, setOrders] = useState<Order[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
    const [currentStatus, setCurrentStatus] = useState<OrderStatus>('PENDING');
    const [isDownloading, setIsDownloading] = useState(false);

    useEffect(() => {
        fetchOrders();
    }, []);

    const fetchOrders = async () => {
        try {
            const response = await axiosClient.get('/orders');
            setOrders(response.data);
        } catch (error) {
            console.error('Failed to fetch orders:', error);
        }
    };

    const handleOpenModal = (order: Order) => {
        setSelectedOrder(order);
        setCurrentStatus(order.status);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setSelectedOrder(null);
    };

    const handleStatusUpdate = async () => {
        if (!selectedOrder) return;
        try {
            await axiosClient.put(`/orders/${selectedOrder.id}/status?status=${currentStatus}`);
            fetchOrders();
            handleCloseModal();
        } catch (error) {
            console.error('Failed to update order status:', error);
            alert('Failed to update status.');
        }
    };

    const handleDownloadInvoice = async (orderId: number) => {
        setIsDownloading(true);
        try {
            try {
                await axiosClient.post(`/invoices/generate/${orderId}?includesVAT=true`);
            } catch (error) {
                const axiosError = error as AxiosError;
                if (axiosError.response?.status !== 409) {
                    throw error;
                }
            }

            const invoiceResponse = await axiosClient.get(`/invoices/by-order/${orderId}`);
            const invoiceId = invoiceResponse.data.id;

            window.open(`http://localhost:8080/api/invoices/pdf/${invoiceId}`);

        } catch (error) {
            console.error('Failed to download invoice:', error);
            alert('Could not download the invoice. Please check the console.');
        } finally {
            setIsDownloading(false);
        }
    };

    return (
        <div>
            <h1 className="text-3xl font-bold text-white mb-6">Manage Orders</h1>

            <div className="bg-card p-4 rounded-lg border border-gray-700">
                {/* --- CORRECTION : LE CODE COMPLET DU TABLEAU EST REMIS ICI --- */}
                <table className="w-full text-left">
                    <thead>
                    <tr className="border-b border-gray-600">
                        <th className="p-4">Order ID</th>
                        <th className="p-4">Customer</th>
                        <th className="p-4">Date</th>
                        <th className="p-4">Total</th>
                        <th className="p-4">Status</th>
                        <th className="p-4">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {orders.map((order) => (
                        <tr key={order.id} className="border-b border-gray-700 hover:bg-gray-800">
                            <td className="p-4 font-mono">#{order.id}</td>
                            <td className="p-4">{order.user.firstName} {order.user.lastName}</td>
                            <td className="p-4">{new Date(order.orderDate).toLocaleDateString()}</td>
                            <td className="p-4">{order.totalAmount.toFixed(2)} DH</td>
                            <td className="p-4"><StatusBadge status={order.status} /></td>
                            <td className="p-4">
                                <button onClick={() => handleOpenModal(order)} className="text-blue-400 hover:text-blue-300">
                                    <Eye size={20} />
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {selectedOrder && (
                <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={`Order Details #${selectedOrder.id}`}>
                    <div className="space-y-4">
                        {/* --- CORRECTION : LE CODE COMPLET DE LA MODALE EST REMIS ICI --- */}
                        <div>
                            <h3 className="font-bold text-white">Customer Information</h3>
                            <p className="text-gray-400">{selectedOrder.user.firstName} {selectedOrder.user.lastName}</p>
                            <p className="text-gray-400">{selectedOrder.user.email}</p>
                        </div>
                        <div>
                            <h3 className="font-bold text-white">Order Items</h3>
                            <ul className="list-disc list-inside text-gray-400 mt-2 space-y-1">
                                {selectedOrder.orderItems.map((item, index) => (
                                    <li key={index}>
                                        {item.quantity} x {item.productName} @ {item.unitPrice.toFixed(2)} DH
                                    </li>
                                ))}
                            </ul>
                        </div>
                        <div className="border-t border-gray-600 pt-4">
                            <p className="font-bold text-white text-lg flex justify-between">
                                <span>Total Amount:</span>
                                <span>{selectedOrder.totalAmount.toFixed(2)} DH</span>
                            </p>
                        </div>

                        <div className="border-t border-gray-600 pt-4">
                            <button
                                onClick={() => handleDownloadInvoice(selectedOrder.id)}
                                disabled={isDownloading}
                                className="w-full flex items-center justify-center bg-green-600 text-white font-bold py-2 rounded-lg hover:bg-green-700 transition-colors disabled:bg-gray-500"
                            >
                                <Download className="w-5 h-5 mr-2" />
                                {isDownloading ? 'Generating...' : 'Download Invoice (PDF)'}
                            </button>
                        </div>

                        <div>
                            <label className="block font-bold text-white mb-2">Update Order Status</label>
                            <select
                                value={currentStatus}
                                onChange={(e) => setCurrentStatus(e.target.value as OrderStatus)}
                                className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600"
                            >
                                {ORDER_STATUSES.map(status => (
                                    <option key={status} value={status}>{status}</option>
                                ))}
                            </select>
                        </div>
                        <div className="flex justify-end space-x-4 pt-4">
                            <button type="button" onClick={handleCloseModal} className="bg-gray-600 text-white px-4 py-2 rounded-lg">Close</button>
                            <button type="button" onClick={handleStatusUpdate} className="bg-primary text-white px-4 py-2 rounded-lg">Save Status</button>
                        </div>
                    </div>
                </Modal>
            )}
        </div>
    );
};

export default Orders;