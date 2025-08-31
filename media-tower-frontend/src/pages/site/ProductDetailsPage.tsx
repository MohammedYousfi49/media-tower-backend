// Fichier : src/pages/site/ProductDetailsPage.tsx (Version complète et corrigée)

// Fichier : src/pages/site/ProductDetailsPage.tsx (Aucun changement nécessaire ici)
import { useEffect, useState, FormEvent } from 'react';
import { useParams, Link } from 'react-router-dom';
import axiosClient from '../../api/axiosClient';
import { useTranslation } from 'react-i18next';
import { useCart } from '../../contexts/CartContext';
import { useAuth } from '../../contexts/AuthContext';
import { AxiosError } from 'axios';
import ShareButtons from '../../components/site/ShareButtons';
import ProductCard from '../../components/site/ProductCard';
import { Star } from 'lucide-react';
import ProductImageGallery from '../../components/site/ProductImageGallery';

// Les interfaces restent les mêmes
interface Media {
  id: number;
  url: string;
  isPrimary: boolean;
}
interface Product {
  id: number;
  names: { [key: string]: string };
  descriptions: { [key: string]: string };
  price: number;
  images: Media[];
  categoryName: string;
  categoryId: number;
}
interface Review {
  id: number;
  userName: string;
  rating: number;
  comment: string;
  reviewDate: string;
}

const ProductDetailsPage = () => {
  const { id } = useParams<{ id: string }>();
  const { t, i18n } = useTranslation();
  const { addToCart } = useCart();
  const { currentUser } = useAuth();

  const [product, setProduct] = useState<Product | null>(null);
  // Ces variables sont maintenant utilisées dans le JSX ci-dessous
  const [reviews, setReviews] = useState<Review[]>([]);
  const [similarProducts, setSimilarProducts] = useState<Product[]>([]);
  const [canReview, setCanReview] = useState(false);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) return;

    const fetchProductData = async () => {
      window.scrollTo(0, 0);
      try {
        // 1. Charger le produit (essentiel)
        const productRes = await axiosClient.get(`/products/${id}`);
        setProduct(productRes.data);

        // 2. Charger les données secondaires (non bloquant)
        axiosClient.get(`/reviews/product/${id}`).then(res => setReviews(res.data)).catch(err => console.error("Failed to fetch reviews:", err));
        axiosClient.get(`/products/${id}/similar`).then(res => setSimilarProducts(res.data)).catch(err => console.error("Failed to fetch similar products:", err));

        if (currentUser) {
          axiosClient.get(`/orders/can-review/${id}`).then(res => setCanReview(res.data.canReview)).catch(err => console.error("Failed to fetch can-review status:", err));
        }

      } catch (error) {
        console.error("Failed to fetch main product details:", error);
        setError("Product not found.");
      }
    };

    fetchProductData();
  }, [id, currentUser]);

  const handleAddToCart = () => {
    if (product) {
      const primaryImage = product.images?.find(img => img.isPrimary) || product.images?.[0];
      addToCart({
        id: product.id,
        name: product.names[i18n.language] || product.names.en,
        price: product.price,
        imageUrl: primaryImage?.url || '',
      });
      alert(`${product.names[i18n.language] || product.names.en} added to cart!`);
    }
  };

  // Cette fonction est maintenant utilisée dans le formulaire ci-dessous
  const handleReviewSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (rating === 0) {
      setError("Please select a rating.");
      return;
    }
    try {
      await axiosClient.post(`/reviews/product/${id}`, { rating, comment });
      // Recharger les avis après soumission
      const reviewsRes = await axiosClient.get(`/reviews/product/${id}`);
      setReviews(reviewsRes.data);
      setRating(0);
      setComment('');
    } catch(err) {
      if (err instanceof AxiosError && err.response) {
        const responseData = err.response.data as { message?: string } | string;
        setError(typeof responseData === 'string' ? responseData : responseData.message || 'Failed to submit review.');
      } else {
        setError('An unexpected error occurred.');
      }
    }
  };

  if (!product && !error) {
    return <div className="text-center py-20">{t('loading')}</div>;
  }
  if (error && !product) { // Affiche l'erreur seulement si le produit n'a pas pu être chargé
    return <div className="text-center py-20 text-red-500">{error}</div>;
  }
  if (!product) {
    return <div className="text-center py-20 text-red-500">Could not load the product.</div>;
  }

  const productName = product.names[i18n.language] || product.names.en;
  const productDescription = product.descriptions[i18n.language] || product.descriptions.en;
  const currentPageUrl = window.location.href;

  return (
      <div className="space-y-12">
        <div className="grid md:grid-cols-2 gap-8">
          <div>
            <ProductImageGallery images={product.images} />
          </div>
          <div className="flex flex-col">
            <p className="text-sm text-gray-400">{product.categoryName}</p>
            <h1 className="text-4xl font-bold text-white mt-2">{productName}</h1>
            <p className="text-gray-300 mt-4 text-lg flex-grow">{productDescription}</p>
            <div className="mt-8 flex items-center justify-between gap-4">
              <span className="text-3xl font-bold text-primary">{product.price.toFixed(2)} DH</span>
              <button onClick={handleAddToCart} className="bg-primary text-primary-foreground font-bold px-6 py-3 rounded-lg hover:bg-opacity-80 transition-colors">
                {t('addToCart')}
              </button>
            </div>
            <div className="mt-8 pt-4 border-t border-secondary">
              <ShareButtons url={currentPageUrl} title={productName} />
            </div>
          </div>
        </div>

        {/* --- DÉBUT DE LA PARTIE JSX RESTAURÉE --- */}
        <section className="border-t border-secondary pt-8">
          <h2 className="text-3xl font-bold text-white mb-6">{t('reviews')}</h2>
          <div className="grid md:grid-cols-2 gap-8">
            <div className="bg-card p-6 rounded-lg border border-secondary">
              <h3 className="font-bold text-xl mb-4">Leave a Review</h3>
              {currentUser ? (
                  canReview ? (
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
                          <textarea value={comment} onChange={(e) => setComment(e.target.value)} className="w-full bg-background text-white p-2 rounded-md border border-secondary" rows={4}/>
                        </div>
                        {error && <p className="text-red-500 text-sm">{error}</p>}
                        <button type="submit" className="w-full bg-primary text-primary-foreground font-bold py-2 rounded-lg">Submit Review</button>
                      </form>
                  ) : (
                      <p className="text-gray-400">You must purchase this product to leave a review.</p>
                  )
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
                  <p className="text-gray-400">No reviews yet for this product.</p>
              )}
            </div>
          </div>
        </section>

        {similarProducts.length > 0 && (
            <section className="border-t border-secondary pt-8">
              <h2 className="text-3xl font-bold text-white mb-6">You might also like</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {similarProducts.map(p => (
                    <ProductCard key={p.id} product={p} />
                ))}
              </div>
            </section>
        )}
        {/* --- FIN DE LA PARTIE JSX RESTAURÉE --- */}
      </div>
  );
};

export default ProductDetailsPage