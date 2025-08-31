// Fichier : src/pages/site/ServiceDetailsPage.tsx (Version avec la correction finale de 'err')

import { useEffect, useState, FormEvent } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import axiosClient from '../../api/axiosClient';
import { useAuth } from '../../hooks/useAuth';
import { useTranslation } from 'react-i18next';
import { Star } from 'lucide-react';
import { AxiosError } from 'axios';
import ShareButtons from '../../components/site/ShareButtons';
import ServiceCard from '../../components/site/ServiceCard';
import ProductImageGallery from '../../components/site/ProductImageGallery';

// Interfaces
interface Media {
    id: number;
    url: string;
    isPrimary: boolean;
}
interface Service {
    id: number;
    names: { [key: string]: string };
    descriptions: { [key: string]: string };
    price: number;
    images: Media[];
}
interface ServiceReview {
    id: number;
    userName: string;
    rating: number;
    comment: string;
    reviewDate: string;
}

const ServiceDetailsPage = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { currentUser } = useAuth();
    const { i18n, t } = useTranslation();

    const [service, setService] = useState<Service | null>(null);
    const [reviews, setReviews] = useState<ServiceReview[]>([]);
    const [similarServices, setSimilarServices] = useState<Service[]>([]);
    const [notes, setNotes] = useState('');
    const [rating, setRating] = useState(0);
    const [comment, setComment] = useState('');
    const [reviewError, setReviewError] = useState('');
    const [bookingLoading, setBookingLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchServiceDetails = async () => {
            if (!id) return;
            window.scrollTo(0, 0);
            try {
                const [serviceRes, reviewsRes, similarRes] = await Promise.all([
                    axiosClient.get(`/services/${id}`),
                    axiosClient.get(`/service-reviews/service/${id}`),
                    axiosClient.get(`/services/${id}/similar`)
                ]);
                setService(serviceRes.data);
                setReviews(reviewsRes.data);
                setSimilarServices(similarRes.data);
            } catch (err) {
                console.error("Failed to fetch service details:", err);
                setError("Failed to load service details. Please try again later.");
            }
        };

        void fetchServiceDetails();

    }, [id]);

    const handleBooking = async () => {
        if (!currentUser) {
            navigate('/login', { state: { from: window.location.pathname } });
            return;
        }
        setBookingLoading(true);
        try {
            await axiosClient.post(`/bookings/service/${id}`, { notes });
            alert('Booking request sent successfully!');
            navigate('/account');
        } catch (err) {
            // La correction est ici
            console.error("Failed to send booking request:", err);
            alert('Failed to send booking request.');
        } finally {
            setBookingLoading(false);
        }
    };

    const handleReviewSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setReviewError('');
        try {
            await axiosClient.post(`/service-reviews/service/${id}`, { rating, comment });
            const reviewsRes = await axiosClient.get(`/service-reviews/service/${id}`);
            setReviews(reviewsRes.data);
            setRating(0);
            setComment('');
        } catch(err) {
            if (err instanceof AxiosError && err.response) {
                setReviewError(err.response.data.message || "Failed to submit review.");
            } else {
                setReviewError('An unexpected error occurred.');
            }
        }
    };

    if (error) return <div className="text-center p-8 text-red-500">{error}</div>;
    if (!service) return <div className="text-center p-8">Loading service...</div>;

    const serviceName = service.names[i18n.language] || service.names.en;
    const serviceDescription = service.descriptions[i18n.language] || service.descriptions.en;
    const currentPageUrl = window.location.href;

    return (
        <div className="space-y-12">
            <div className="grid md:grid-cols-2 gap-8 md:gap-12">
                <div>
                    <ProductImageGallery images={service.images} />
                </div>
                <div>
                    <h1 className="text-3xl md:text-4xl font-bold text-white">{serviceName}</h1>
                    <p className="text-2xl text-primary my-4 font-semibold">{service.price.toFixed(2)} DH</p>
                    <p className="text-gray-300 leading-relaxed">{serviceDescription}</p>
                    <div className="mt-8 bg-card p-6 rounded-lg border border-secondary">
                        <h2 className="font-bold text-xl mb-4 text-white">Request this Service</h2>
                        <textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder="Add any notes for your request (optional)..." className="w-full bg-background p-2 rounded mb-4" rows={3}></textarea>
                        <button onClick={handleBooking} disabled={bookingLoading} className="w-full bg-primary py-3 rounded text-white font-bold hover:bg-opacity-80 disabled:bg-gray-500">
                            {bookingLoading ? 'Sending...' : 'Send Booking Request'}
                        </button>
                    </div>
                    <div className="mt-8 pt-4 border-t border-secondary">
                        <ShareButtons url={currentPageUrl} title={serviceName} />
                    </div>
                </div>
            </div>

            <section className="border-t border-secondary pt-8">
                <h2 className="text-3xl font-bold text-white mb-6">{t('reviews')}</h2>
                <div className="grid md:grid-cols-2 gap-8">
                    <div className="bg-card p-6 rounded-lg border border-secondary">
                        <h3 className="font-bold text-xl mb-4">Leave a Review</h3>
                        {currentUser ? (
                            <form onSubmit={handleReviewSubmit} className="space-y-4">
                                <div>
                                    <label className="block text-gray-400 mb-2">{t('rating')}</label>
                                    <div className="flex space-x-1">
                                        {[1, 2, 3, 4, 5].map(star => (
                                            <Star key={star} onClick={() => setRating(star)} className={`cursor-pointer transition-colors ${rating >= star ? 'text-yellow-400' : 'text-gray-500'}`} fill={rating >= star ? 'currentColor' : 'none'}/>
                                        ))}
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-gray-400 mb-1">{t('comment')}</label>
                                    <textarea value={comment} onChange={(e) => setComment(e.target.value)} className="w-full bg-background p-2 rounded-md" rows={4}/>
                                </div>
                                {reviewError && <p className="text-red-500 text-sm">{reviewError}</p>}
                                <button type="submit" className="w-full bg-primary text-white font-bold py-2 rounded-lg">Submit Review</button>
                            </form>
                        ) : (
                            <p className="text-gray-400">{t('loginToReview')} <Link to="/login" className="text-primary hover:underline">Login here</Link>.</p>
                        )}
                    </div>
                    <div className="space-y-4">
                        {reviews.length > 0 ? (
                            reviews.map(review => (
                                <div key={review.id} className="bg-card p-4 rounded-lg border border-secondary">
                                    <div className="flex justify-between items-center">
                                        <span className="font-bold text-white">{review.userName}</span>
                                        <div className="flex">
                                            {[...Array(5)].map((_, i) => (
                                                <Star key={i} size={16} className={i < review.rating ? 'text-yellow-400' : 'text-gray-500'} fill="currentColor"/>
                                            ))}
                                        </div>
                                    </div>
                                    <p className="text-gray-300 mt-2">{review.comment}</p>
                                </div>
                            ))
                        ) : (
                            <p className="text-gray-400">No reviews yet for this service.</p>
                        )}
                    </div>
                </div>
            </section>

            {similarServices.length > 0 && (
                <section className="border-t border-secondary pt-8">
                    <h2 className="text-3xl font-bold text-white mb-6">Other Services You May Like</h2>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                        {similarServices.map(s => (
                            <ServiceCard key={s.id} service={s} />
                        ))}
                    </div>
                </section>
            )}
        </div>
    );
};

export default ServiceDetailsPage;