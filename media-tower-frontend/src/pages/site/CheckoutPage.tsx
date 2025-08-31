import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../../contexts/CartContext';
import { useAuth } from '../../hooks/useAuth';
import { useTranslation } from 'react-i18next';
import axiosClient from '../../api/axiosClient';

const CheckoutPage = () => {
    const { cartItems, totalPrice, clearCart } = useCart();
    const { currentUser } = useAuth();
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [paymentMethod, setPaymentMethod] = useState('COD'); // COD = Cash on Delivery
    const [loading, setLoading] = useState(false);

    const handlePlaceOrder = async () => {
        if (!currentUser) {
            alert(t('loginToCheckout'));
            navigate('/login');
            return;
        }
        setLoading(true);
        try {
            const orderDto = {
                orderItems: cartItems.map(item => ({
                    productId: item.id,
                    quantity: item.quantity,
                })),
                paymentMethod: paymentMethod,
            };

            const response = await axiosClient.post('/orders', orderDto);
            const orderId = response.data.id;
            
            // Si le paiement est en ligne, il faudrait rediriger vers Stripe/Paypal.
            // Pour cet exemple, on suppose que le backend gère ça ou on va directement à la confirmation.
            
            clearCart();
            navigate(`/order-confirmation/${orderId}`);

        } catch (error) {
            console.error("Failed to place order:", error);
            alert("An error occurred while placing your order.");
        } finally {
            setLoading(false);
        }
    };

    if (cartItems.length === 0) {
        navigate('/products');
        return null;
    }

    return (
        <div>
            <h1 className="text-4xl font-bold mb-8">{t('checkout')}</h1>
            <div className="grid md:grid-cols-2 gap-8">
                {/* Détails de la commande */}
                <div className="bg-card p-6 rounded-lg border border-gray-700">
                    <h2 className="text-2xl font-bold mb-4">Your Order</h2>
                    <div className="space-y-2">
                        {cartItems.map(item => (
                            <div key={item.id} className="flex justify-between">
                                <span>{item.name} x {item.quantity}</span>
                                <span>{(item.price * item.quantity).toFixed(2)} DH</span>
                            </div>
                        ))}
                    </div>
                    <div className="border-t border-gray-600 mt-4 pt-4 flex justify-between font-bold text-xl">
                        <span>{t('total')}</span>
                        <span>{totalPrice.toFixed(2)} DH</span>
                    </div>
                </div>

                {/* Options de paiement */}
                <div className="bg-card p-6 rounded-lg border border-gray-700">
                    <h2 className="text-2xl font-bold mb-4">Payment Method</h2>
                    <div className="space-y-3">
                         <label className="flex items-center p-3 border border-gray-600 rounded-lg has-[:checked]:bg-primary has-[:checked]:border-blue-500">
                            <input type="radio" name="paymentMethod" value="COD" checked={paymentMethod === 'COD'} onChange={e => setPaymentMethod(e.target.value)} className="w-4 h-4 mr-3"/>
                            <span>Pay on Delivery</span>
                        </label>
                         <label className="flex items-center p-3 border border-gray-600 rounded-lg has-[:checked]:bg-primary has-[:checked]:border-blue-500">
                            <input type="radio" name="paymentMethod" value="STRIPE" checked={paymentMethod === 'STRIPE'} onChange={e => setPaymentMethod(e.target.value)} className="w-4 h-4 mr-3"/>
                            <span>Credit Card (Stripe)</span>
                        </label>
                         <label className="flex items-center p-3 border border-gray-600 rounded-lg has-[:checked]:bg-primary has-[:checked]:border-blue-500">
                            <input type="radio" name="paymentMethod" value="PAYPAL" checked={paymentMethod === 'PAYPAL'} onChange={e => setPaymentMethod(e.target.value)} className="w-4 h-4 mr-3"/>
                            <span>PayPal</span>
                        </label>
                    </div>
                    <button 
                        onClick={handlePlaceOrder}
                        disabled={loading}
                        className="w-full mt-6 bg-primary text-white font-bold py-3 rounded-lg hover:bg-blue-600 disabled:bg-gray-500"
                    >
                        {loading ? 'Processing...' : t('placeOrder')}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default CheckoutPage;