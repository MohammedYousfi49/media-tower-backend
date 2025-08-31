import { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import { Edit } from 'lucide-react';
import Modal from '../../components/shared/Modal';

interface Reservation {
    id: number;
    userEmail: string;
    productName: string;
    reservationDate: string;
    status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
    notes: string;
}

const RESERVATION_STATUSES: Reservation['status'][] = ['PENDING', 'CONFIRMED', 'CANCELLED'];

const Reservations = () => {
    const [reservations, setReservations] = useState<Reservation[]>([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentReservation, setCurrentReservation] = useState<Reservation | null>(null);
    const [selectedStatus, setSelectedStatus] = useState<Reservation['status']>('PENDING');

    useEffect(() => {
        fetchReservations();
    }, []);

    const fetchReservations = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get<Reservation[]>('/reservations');
            setReservations(response.data);
        } catch (error) {
            console.error('Failed to fetch reservations:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleOpenModal = (reservation: Reservation) => {
        setCurrentReservation(reservation);
        setSelectedStatus(reservation.status);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => setIsModalOpen(false);

    const handleSaveStatus = async () => {
        if (!currentReservation) return;
        try {
            await axiosClient.put(`/reservations/${currentReservation.id}/status`, { status: selectedStatus });
            fetchReservations();
            handleCloseModal();
        } catch (error) {
            console.error('Failed to update reservation status:', error);
            alert('Failed to update status.');
        }
    };

    return (
        <div>
            <h1 className="text-3xl font-bold text-white mb-6">Manage Reservations</h1>
            <div className="bg-card p-4 rounded-lg border border-gray-700 overflow-x-auto">
                <table className="w-full text-left min-w-[800px]">
                    <thead>
                    <tr className="border-b border-gray-600">
                        <th className="p-3">ID</th>
                        <th className="p-3">Customer</th>
                        <th className="p-3">Product/Service</th>
                        <th className="p-3">Date</th>
                        <th className="p-3">Status</th>
                        <th className="p-3">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={6} className="text-center p-4">Loading reservations...</td></tr>
                    ) : reservations.map((res) => (
                        <tr key={res.id} className="border-b border-gray-700 hover:bg-gray-800">
                            <td className="p-3 font-mono">#{res.id}</td>
                            <td className="p-3">{res.userEmail}</td>
                            <td className="p-3">{res.productName}</td>
                            <td className="p-3">{new Date(res.reservationDate).toLocaleString()}</td>
                            <td className="p-3">{res.status}</td>
                            <td className="p-3">
                                <button onClick={() => handleOpenModal(res)} className="text-yellow-400 hover:text-yellow-300"><Edit size={20} /></button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {currentReservation && (
                <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={`Reservation #${currentReservation.id}`}>
                    <div className="space-y-4">
                        <p><strong>Customer:</strong> {currentReservation.userEmail}</p>
                        <p><strong>Service:</strong> {currentReservation.productName}</p>
                        <p><strong>Date:</strong> {new Date(currentReservation.reservationDate).toLocaleString()}</p>
                        <p><strong>Notes:</strong> {currentReservation.notes || 'None'}</p>
                        <div>
                            <label className="block font-bold text-white mb-2">Update Status</label>
                            <select
                                value={selectedStatus}
                                onChange={(e) => setSelectedStatus(e.target.value as Reservation['status'])}
                                className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600"
                            >
                                {RESERVATION_STATUSES.map(status => (
                                    <option key={status} value={status}>{status}</option>
                                ))}
                            </select>
                        </div>
                        <div className="flex justify-end space-x-4 pt-4 border-t border-gray-700">
                            <button type="button" onClick={handleCloseModal} className="bg-gray-600 text-white px-4 py-2 rounded-lg">Close</button>
                            <button type="button" onClick={handleSaveStatus} className="bg-primary text-white px-4 py-2 rounded-lg">Save Status</button>
                        </div>
                    </div>
                </Modal>
            )}
        </div>
    );
};

export default Reservations;