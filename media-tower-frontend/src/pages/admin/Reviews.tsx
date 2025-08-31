// Fichier : src/pages/admin/Reviews.tsx (COMPLET)

import { useEffect, useState } from 'react';
import { Trash2, Loader2, Star } from 'lucide-react';
import axiosClient from '../../api/axiosClient';

// Interface pour le DTO unifié que nous recevons du backend
interface AdminReview {
    id: number;
    type: 'Product' | 'Service';
    sourceId: number;
    sourceName: string;
    userName: string;
    rating: number;
    comment: string;
    reviewDate: string; // La date sera une chaîne de caractères (format ISO)
}

// Composant pour afficher les étoiles
const StarRating = ({ rating }: { rating: number }) => (
    <div className="flex">
        {[...Array(5)].map((_, i) => (
            <Star key={i} size={16} className={i < rating ? 'text-yellow-400 fill-current' : 'text-gray-500'} />
        ))}
    </div>
);

const Reviews = () => {
    const [reviews, setReviews] = useState<AdminReview[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchReviews = async () => {
        setLoading(true);
        try {
            const response = await axiosClient.get<AdminReview[]>('/admin/reviews');
            setReviews(response.data);
        } catch (error) {
            console.error('Failed to fetch reviews:', error);
            alert('Could not load reviews.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void fetchReviews();
    }, []);

    const handleDelete = async (type: string, id: number) => {
        if (window.confirm('Are you sure you want to permanently delete this review?')) {
            try {
                await axiosClient.delete(`/admin/reviews/${type.toLowerCase()}/${id}`);
                setReviews(prev => prev.filter(review => !(review.id === id && review.type === type)));
                alert('Review deleted successfully.');
            } catch (error) {
                console.error('Failed to delete review:', error);
                alert('Could not delete the review.');
            }
        }
    };

    return (
        <div>
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-white">Manage Reviews</h1>
            </div>

            <div className="bg-card p-4 rounded-lg border border-gray-700">
                <table className="w-full text-left">
                    <thead>
                    <tr className="border-b border-gray-600 text-sm text-gray-400">
                        <th className="p-2">Source</th>
                        <th className="p-2">User</th>
                        <th className="p-2">Rating</th>
                        <th className="p-2">Comment</th>
                        <th className="p-2">Date</th>
                        <th className="p-2 text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={6} className="text-center p-8"><Loader2 className="animate-spin inline-block" /></td></tr>
                    ) : reviews.map((review) => (
                        <tr key={`${review.type}-${review.id}`} className="border-b border-gray-700 hover:bg-gray-800 text-sm">
                            <td className="p-2">
                                    <span className={`px-2 py-1 text-xs rounded-full ${review.type === 'Product' ? 'bg-blue-800 text-blue-200' : 'bg-green-800 text-green-200'}`}>
                                        {review.type}
                                    </span>
                                <p className="font-semibold text-white mt-1">{review.sourceName}</p>
                            </td>
                            <td className="p-2">{review.userName}</td>
                            <td className="p-2"><StarRating rating={review.rating} /></td>
                            <td className="p-2 text-gray-300 max-w-sm truncate">{review.comment}</td>
                            <td className="p-2">{new Date(review.reviewDate).toLocaleDateString()}</td>
                            <td className="p-2 text-center">
                                <button
                                    onClick={() => handleDelete(review.type, review.id)}
                                    className="text-red-500 hover:text-red-400"
                                    title="Delete Review"
                                >
                                    <Trash2 size={20} />
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default Reviews;