import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
// CORRECTION : BarChart et Bar ont été supprimés
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { ShoppingBag, Users, Package, DollarSign, BookOpen } from 'lucide-react';
import Card from '../../components/ui/Card';
import axiosClient from '../../api/axiosClient';

// Interfaces pour les données du dashboard
interface KpiDto {
    totalUsers: number;
    totalProducts: number;
    totalServices: number;
    totalOrders: number;
    totalBookings: number;
}
interface RevenueDto {
    totalRevenue: number;
    revenueLast30Days: number;
}
interface ChartDataPoint {
    date: string;
    amount: number;
}
interface RecentActivity {
    id: number;
    customerName?: string;
    user?: { firstName: string, lastName: string };
    serviceName?: string;
    totalAmount: number;
}
interface DashboardData {
    kpis: KpiDto;
    revenue: RevenueDto;
    salesLast30Days: ChartDataPoint[];
    recentBookings: RecentActivity[];
    recentOrders: RecentActivity[];
    popularServices: { names: { [key: string]: string } }[];
}

const Dashboard = () => {
    const [summary, setSummary] = useState<DashboardData | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await axiosClient.get<DashboardData>('/stats/dashboard-summary');
                setSummary(response.data);
            } catch (error) {
                console.error("Failed to fetch dashboard data", error);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    if (loading) return <div className="flex justify-center items-center h-full">Loading dashboard...</div>;
    if (!summary) return <div className="text-center">Could not load dashboard data.</div>;

    const { kpis, revenue, salesLast30Days, recentOrders, recentBookings, popularServices } = summary;

    const formattedChartData = salesLast30Days.map(d => ({
        date: new Date(d.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        Revenue: d.amount,
    }));

    return (
        <div className="space-y-6">
            <h1 className="text-3xl font-bold text-white">Dashboard</h1>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
                <Card><StatCard icon={DollarSign} title="Total Revenue" value={`${revenue.totalRevenue.toFixed(2)} DH`} color="text-green-400" /></Card>
                <Card><StatCard icon={Users} title="Total Users" value={kpis.totalUsers} color="text-blue-400" /></Card>
                <Card><StatCard icon={ShoppingBag} title="Total Products" value={kpis.totalProducts} color="text-purple-400" /></Card>
                <Card><StatCard icon={BookOpen} title="Total Services" value={kpis.totalServices} color="text-yellow-400" /></Card>
                <Card><StatCard icon={Package} title="Total Orders" value={kpis.totalOrders} color="text-indigo-400" /></Card>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2">
                    <Card>
                        <h2 className="text-xl font-bold mb-4 text-white">Revenue (Last 30 Days)</h2>
                        <div className="h-72">
                            <ResponsiveContainer width="100%" height="100%">
                                <LineChart data={formattedChartData} margin={{ top: 5, right: 20, left: -10, bottom: 5 }}>
                                    <CartesianGrid strokeDasharray="3 3" strokeOpacity={0.2} />
                                    <XAxis dataKey="date" stroke="#888888" fontSize={12} />
                                    <YAxis stroke="#888888" fontSize={12} />
                                    <Tooltip contentStyle={{ backgroundColor: '#1f2937', border: '1px solid #374151' }} />
                                    <Legend />
                                    <Line type="monotone" dataKey="Revenue" stroke="#3b82f6" strokeWidth={2} dot={{ r: 4 }} activeDot={{ r: 8 }} />
                                </LineChart>
                            </ResponsiveContainer>
                        </div>
                    </Card>
                </div>
                <div>
                    <Card>
                        <h2 className="text-xl font-bold mb-4 text-white">Popular Services</h2>
                        <ul className="space-y-3">
                            {popularServices.map((service, index) => (
                                <li key={index} className="flex justify-between items-center text-sm">
                                    <span>{service.names.en || service.names.fr}</span>
                                </li>
                            ))}
                        </ul>
                    </Card>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                    <h2 className="text-xl font-bold mb-4 text-white">Recent Orders</h2>
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <tbody>
                            {recentOrders.map(order => (
                                <tr key={order.id} className="border-b border-gray-700">
                                    <td className="p-2">
                                        <Link to={`/admin/orders`} className="hover:text-primary font-mono">#{order.id}</Link>
                                    </td>
                                    <td className="p-2">{order.user?.firstName} {order.user?.lastName}</td>
                                    <td className="p-2 text-right">{order.totalAmount.toFixed(2)} DH</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </Card>
                <Card>
                    <h2 className="text-xl font-bold mb-4 text-white">Recent Bookings</h2>
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <tbody>
                            {recentBookings.map(booking => (
                                <tr key={booking.id} className="border-b border-gray-700">
                                    <td className="p-2">
                                        <Link to={`/admin/bookings`} className="hover:text-primary font-mono">#{booking.id}</Link>
                                    </td>
                                    <td className="p-2">{booking.customerName}</td>
                                    <td className="p-2">{booking.serviceName}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </Card>
            </div>
        </div>
    );
};

const StatCard = ({ icon: Icon, title, value, color }: { icon: React.ElementType, title: string, value: string | number, color: string }) => (
    <div className="flex items-center">
        <Icon className={`w-8 h-8 ${color} mr-4`} />
        <div>
            <p className="text-gray-400">{title}</p>
            <p className="text-2xl font-bold">{value}</p>
        </div>
    </div>
);

export default Dashboard;