// Fichier : src/pages/admin/Users.tsx (COMPLET ET FINAL)

import { useEffect, useState } from 'react';
import { Edit, Trash2, UserX, UserCheck } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Modal from '../../components/shared/Modal';

interface AppUser {
    id: number;
    uid: string;
    email: string;
    firstName: string;
    lastName: string;
    role: 'USER' | 'ADMIN' | 'SELLER';
    status: 'ACTIVE' | 'BLOCKED';
    phoneNumber: string;
    address: string;
}

const Users = () => {
    const [users, setUsers] = useState<AppUser[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentUser, setCurrentUser] = useState<AppUser | null>(null);
    const [selectedRole, setSelectedRole] = useState<'USER' | 'ADMIN' | 'SELLER'>('USER');
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        void fetchUsers();
    }, []);

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get<AppUser[]>('/users');
            setUsers(response.data);
        } catch (error) {
            console.error('Failed to fetch users:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleOpenModal = (user: AppUser) => {
        setCurrentUser(user);
        setSelectedRole(user.role);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setCurrentUser(null);
    };

    const handleRoleChange = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!currentUser) return;
        try {
            await axiosClient.put(`/users/${currentUser.id}/role`, { role: selectedRole });
            await fetchUsers();
            handleCloseModal();
        } catch (error) {
            console.error('Failed to update user role:', error);
            alert('Failed to update role.');
        }
    };

    const handleToggleStatus = async (user: AppUser) => {
        const newStatus = user.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
        if (window.confirm(`Are you sure you want to set status to ${newStatus} for ${user.email}?`)) {
            try {
                await axiosClient.put(`/users/${user.id}/status`, { status: newStatus });
                await fetchUsers();
            } catch (error) {
                console.error('Failed to update user status:', error);
                alert('Failed to update status.');
            }
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
            try {
                await axiosClient.delete(`/users/${id}`);
                await fetchUsers();
            } catch (error) {
                console.error('Failed to delete user:', error);
            }
        }
    };

    const filteredUsers = users.filter(user =>
        user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (user.firstName && user.firstName.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (user.lastName && user.lastName.toLowerCase().includes(searchTerm.toLowerCase()))
    );

    const RoleBadge = ({ role }: { role: string }) => {
        const roleStyles: { [key: string]: string } = {
            ADMIN: 'bg-red-500 text-white',
            SELLER: 'bg-yellow-500 text-black',
            USER: 'bg-blue-500 text-white',
        };
        return <span className={`px-2 py-1 text-xs font-bold rounded-full ${roleStyles[role]}`}>{role}</span>;
    };

    const StatusBadge = ({ status, user }: { status: string, user: AppUser }) => {
        const isActive = status === 'ACTIVE';
        return (
            <button onClick={() => handleToggleStatus(user)} title={`Set to ${isActive ? 'BLOCKED' : 'ACTIVE'}`}
                    className={`flex items-center text-sm font-semibold p-1 rounded-md transition-colors ${
                        isActive ? 'text-green-300 hover:bg-green-800' : 'text-red-400 hover:bg-red-800'
                    }`}
            >
                {isActive ? <UserCheck size={16} className="mr-1"/> : <UserX size={16} className="mr-1"/>}
                {isActive ? 'Active' : 'Blocked'}
            </button>
        );
    };

    return (
        <div>
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-white">Manage Users</h1>
                <input
                    type="text"
                    placeholder="Search by name or email..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="bg-gray-700 text-white px-4 py-2 rounded-lg border border-gray-600 focus:outline-none"
                />
            </div>

            <div className="bg-card p-4 rounded-lg border border-gray-700">
                <table className="w-full text-left">
                    <thead>
                    <tr className="border-b border-gray-600">
                        <th className="p-4">Full Name</th>
                        <th className="p-4">Email</th>
                        <th className="p-4">Role</th>
                        <th className="p-4">Status</th>
                        <th className="p-4 text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={5} className="text-center p-4">Loading...</td></tr>
                    ) : filteredUsers.map((user) => (
                        <tr key={user.id} className="border-b border-gray-700 hover:bg-gray-800">
                            <td className="p-4">
                                {user.firstName || user.lastName ? `${user.firstName || ''} ${user.lastName || ''}`.trim() : <span className="text-gray-500">Not provided</span>}
                            </td>
                            <td className="p-4">{user.email}</td>
                            <td className="p-4"><RoleBadge role={user.role} /></td>
                            <td className="p-4"><StatusBadge status={user.status} user={user}/></td>
                            <td className="p-4 flex justify-center space-x-4">
                                <button onClick={() => handleOpenModal(user)} title="Edit Role" className="text-yellow-400 hover:text-yellow-300"><Edit size={20} /></button>
                                <button onClick={() => handleDelete(user.id)} title="Delete User" className="text-red-500 hover:text-red-400"><Trash2 size={20} /></button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {isModalOpen && currentUser && (
                <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={`Edit Role for ${currentUser?.email}`}>
                    <form onSubmit={handleRoleChange}>
                        <div className="mb-4">
                            <label className="block text-gray-400 mb-1">User Role</label>
                            <select
                                defaultValue={currentUser.role}
                                onChange={(e) => setSelectedRole(e.target.value as 'USER' | 'ADMIN' | 'SELLER')}
                                className="w-full bg-gray-700 text-white p-2 rounded-md border border-gray-600"
                            >
                                <option value="USER">User</option>
                                <option value="SELLER">Seller</option>
                                <option value="ADMIN">Admin</option>
                            </select>
                        </div>
                        <div className="flex justify-end space-x-4 mt-6">
                            <button type="button" onClick={handleCloseModal} className="bg-gray-600 text-white px-4 py-2 rounded-lg">Cancel</button>
                            <button type="submit" className="bg-primary text-white px-4 py-2 rounded-lg">Save Role</button>
                        </div>
                    </form>
                </Modal>
            )}
        </div>
    );
};

export default Users;