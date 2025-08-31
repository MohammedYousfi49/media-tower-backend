import { Link } from 'react-router-dom';
import { useCart } from '../../contexts/CartContext';
import { useTranslation } from 'react-i18next';
import { Trash2 } from 'lucide-react';

const CartPage = () => {
    const { cartItems, removeFromCart, updateQuantity, totalPrice, cartCount } = useCart();
    const { t } = useTranslation();

    return (
        <div>
            <h1 className="text-4xl font-bold mb-8">{t('shoppingCart')} ({cartCount})</h1>
            {cartItems.length > 0 ? (
                <div className="grid md:grid-cols-3 gap-8">
                    {/* Liste des articles */}
                    <div className="md:col-span-2 space-y-4">
                        {cartItems.map(item => (
                            <div key={item.id} className="bg-card p-4 rounded-lg flex items-center justify-between border border-gray-700">
                                <div className="flex items-center">
                                    <img src={item.imageUrl || 'https://via.placeholder.com/80'} alt={item.name} className="w-20 h-20 rounded-md object-cover mr-4"/>
                                    <div>
                                        <h2 className="text-lg font-bold text-white">{item.name}</h2>
                                        <p className="text-gray-400">{item.price.toFixed(2)} DH</p>
                                    </div>
                                </div>
                                <div className="flex items-center space-x-4">
                                    <input
                                        type="number"
                                        value={item.quantity}
                                        onChange={(e) => updateQuantity(item.id, parseInt(e.target.value))}
                                        className="w-16 bg-gray-700 text-white text-center p-1 rounded-md border border-gray-600"
                                        min="1"
                                    />
                                    <p className="font-bold w-24 text-right">{(item.price * item.quantity).toFixed(2)} DH</p>
                                    <button onClick={() => removeFromCart(item.id)} className="text-red-500 hover:text-red-400">
                                        <Trash2/>
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Résumé de la commande */}
                    <div className="md:col-span-1">
                        <div className="bg-card p-6 rounded-lg border border-gray-700 space-y-4">
                            <h2 className="text-2xl font-bold">Summary</h2>
                            <div className="flex justify-between text-lg">
                                <span>{t('total')}</span>
                                <span className="font-bold">{totalPrice.toFixed(2)} DH</span>
                            </div>
                             <Link to="/checkout" className="block w-full text-center bg-primary text-white font-bold py-3 rounded-lg hover:bg-blue-600">
                                {t('proceedToCheckout')}
                            </Link>
                        </div>
                    </div>
                </div>
            ) : (
                <div className="text-center py-16">
                    <p className="text-2xl text-gray-400">{t('emptyCart')}</p>
                    <Link to="/products" className="mt-4 inline-block bg-primary text-white font-bold px-6 py-2 rounded-lg">
                        Browse Products
                    </Link>
                </div>
            )}
        </div>
    );
};

export default CartPage;