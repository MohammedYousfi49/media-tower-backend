import { useEffect, useState } from 'react';
import { Eye, Check, X, Clock, PlayCircle, UserCheck, UserPlus } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Modal from '../../components/shared/Modal';
import { useAuth } from '../../hooks/useAuth';

// Interfaces pour les données
interface Booking {
    id: number;
    customerName: string;
    customerEmail: string;
    serviceName: string;
    status: 'PENDING' | 'PROCESSING' | 'CONFIRMED' | 'PAID' | 'COMPLETED' | 'CANCELLED';
    assignedAdminName: string | null;
    customerNotes: string;
    createdAt: string;
}

// Configuration pour l'affichage des statuts
const statusConfig: { [key in Booking['status']]: { color: string; icon: React.ElementType; label: string } } = {
    PENDING: { color: 'text-yellow-400 bg-yellow-900/50', icon: Clock, label: 'Pending' },
    PROCESSING: { color: 'text-blue-400 bg-blue-900/50', icon: PlayCircle, label: 'Processing' },
    CONFIRMED: { color: 'text-purple-400 bg-purple-900/50', icon: UserCheck, label: 'Confirmed' },
    PAID: { color: 'text-green-400 bg-green-900/50', icon: Check, label: 'Paid' },
    COMPLETED: { color: 'text-green-300 bg-green-800/50', icon: Check, label: 'Completed' },
    CANCELLED: { color: 'text-red-400 bg-red-900/50', icon: X, label: 'Cancelled' },
};

// Composant pour le badge de statut
const StatusBadge = ({ status }: { status: Booking['status'] }) => {
    const config = statusConfig[status] || statusConfig.PENDING;
    const Icon = config.icon;
    return (
        <span className={`flex items-center text-xs font-medium px-2.5 py-0.5 rounded-full ${config.color}`}>
            <Icon size={14} className="mr-1.5"/>{config.label}
        </span>
    );
};

const Bookings = () => {
    const { appUser } = useAuth();
    const [bookings, setBookings] = useState<Booking[]>([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null);

    useEffect(() => {
        fetchBookings();
    }, []);

    const fetchBookings = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get<Booking[]>('/bookings');
            setBookings(response.data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
        } catch (error) { console.error('Failed to fetch bookings:', error); }
        finally { setLoading(false); }
    };

    const handleAssign = async (bookingId: number) => {
        try {
            await axiosClient.put(`/bookings/${bookingId}/assign`);
            fetchBookings();
        } catch (error) {
            console.error('Failed to assign booking:', error);
            alert('Failed to assign booking. It might already be processed.');
        }
    };

    const handleUpdateStatus = async (bookingId: number, status: Booking['status']) => {
        try {
            await axiosClient.put(`/bookings/${bookingId}/status`, { status });
            fetchBookings();
            setIsModalOpen(false);
        } catch (error) { console.error('Failed to update status:', error); }
    };

    return (
        <div>
            <h1 className="text-3xl font-bold text-white mb-6">Manage Bookings</h1>
            <div className="bg-card p-4 rounded-lg border border-gray-700 overflow-x-auto">
                <table className="w-full text-left min-w-[800px]">
                    <thead>
                    <tr className="border-b border-gray-600 text-sm text-gray-400">
                        <th className="p-3 font-semibold">Order ID</th>
                        <th className="p-3 font-semibold">Customer</th>
                        <th className="p-3 font-semibold">Service</th>
                        <th className="p-3 font-semibold">Date</th>
                        <th className="p-3 font-semibold">Status</th>
                        <th className="p-3 font-semibold">Assigned To</th>
                        <th className="p-3 font-semibold text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={7} className="text-center p-4">Loading...</td></tr>
                    ) : bookings.map(booking => (
                        <tr key={booking.id} className="border-b border-gray-700 hover:bg-gray-800 text-sm">
                            <td className="p-3 font-mono">#{booking.id}</td>
                            <td className="p-3">{booking.customerName}</td>
                            <td className="p-3">{booking.serviceName}</td>
                            <td className="p-3">{new Date(booking.createdAt).toLocaleDateString()}</td>
                            <td className="p-3"><StatusBadge status={booking.status} /></td>
                            <td className="p-3">{booking.assignedAdminName || '-'}</td>
                            <td className="p-3 flex items-center justify-center space-x-3">
                                <button onClick={() => { setSelectedBooking(booking); setIsModalOpen(true); }} className="hover:text-primary" title="View Details"><Eye /></button>
                                {booking.status === 'PENDING' && (
                                    <button onClick={() => handleAssign(booking.id)} className="flex items-center text-xs bg-blue-600 text-white px-2 py-1 rounded hover:bg-blue-700" title="Assign to me">
                                        <UserPlus size={14} className="mr-1"/> Assign
                                    </button>
                                )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {selectedBooking && (
                <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={`Booking #${selectedBooking.id}`}>
                    <div className="space-y-4 text-gray-300">
                        <div>
                            <h3 className="font-bold text-lg text-white">Customer</h3>
                            <p>{selectedBooking.customerName} ({selectedBooking.customerEmail})</p>
                        </div>
                        <div>
                            <h3 className="font-bold text-lg text-white">Service</h3>
                            <p>{selectedBooking.serviceName}</p>
                        </div>
                        <div className="bg-gray-800 p-3 rounded">
                            <p className="text-sm text-gray-400 mb-1">Customer Notes:</p>
                            <p className="whitespace-pre-wrap">{selectedBooking.customerNotes || 'N/A'}</p>
                        </div>
                        <div className="flex items-center space-x-2">
                            <h3 className="font-bold text-lg text-white">Status:</h3>
                            <StatusBadge status={selectedBooking.status} />
                        </div>
                        {selectedBooking.assignedAdminName && <p><strong>Assigned To:</strong> {selectedBooking.assignedAdminName}</p>}

                        <div className="mt-4 pt-4 border-t border-gray-600">
                            <h3 className="font-bold text-lg text-white mb-2">Actions</h3>
                            <div className="flex flex-wrap gap-2">
                                {(selectedBooking.status === 'PROCESSING' && selectedBooking.assignedAdminName === appUser?.firstName) && (
                                    <button onClick={() => handleUpdateStatus(selectedBooking.id, 'CONFIRMED')} className="bg-green-600 text-white px-3 py-1 rounded hover:bg-green-700">Confirm & Request Payment</button>
                                )}
                                {selectedBooking.status === 'PAID' && (
                                    <button onClick={() => handleUpdateStatus(selectedBooking.id, 'COMPLETED')} className="bg-purple-600 text-white px-3 py-1 rounded hover:bg-purple-700">Mark as Completed</button>
                                )}
                                {(selectedBooking.status === 'PENDING' || selectedBooking.status === 'PROCESSING') && (
                                    <button onClick={() => handleUpdateStatus(selectedBooking.id, 'CANCELLED')} className="bg-red-600 text-white px-3 py-1 rounded hover:bg-red-700">Cancel Booking</button>
                                )}
                            </div>
                        </div>
                    </div>
                </Modal>
            )}
        </div>
    );
};

export default Bookings;