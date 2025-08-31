import { useState, ReactNode } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { ShoppingCart, User, LogOut, Menu, X, Languages, Facebook, Twitter, Instagram } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';
import { useSettings } from '../../contexts/SettingsContext';
import { auth } from '../../lib/firebase';
import { useTranslation } from 'react-i18next';
import ChatBubble from '../site/ChatBubble';

const SiteLayout = ({ children }: { children: ReactNode }) => {
    const { currentUser, appUser } = useAuth();
    const { cartCount } = useCart();
    const { getSetting } = useSettings();
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    const facebookUrl = getSetting('social_facebook_url');
    const twitterUrl = getSetting('social_twitter_url');
    const instagramUrl = getSetting('social_instagram_url');
    const logoUrl = getSetting('site_logo_url');

    const handleLogout = async () => {
        await auth.signOut();
        navigate('/');
    };

    const changeLanguage = (lng: string) => {
        i18n.changeLanguage(lng);
        setIsMenuOpen(false);
    };

    return (
        <div className="min-h-screen bg-background text-foreground flex flex-col">
            <header className="bg-card border-b border-border sticky top-0 z-40">
                <div className="container mx-auto px-4">
                    <div className="flex justify-between items-center py-4">
                        <Link to="/" className="text-2xl font-bold text-white flex items-center gap-2">
                            {logoUrl && logoUrl !== '/logo.png' && <img src={logoUrl} alt="Media Tower Logo" className="h-8"/>}
                            Media Tower
                        </Link>
                        <nav className="hidden md:flex items-center space-x-6">
                            <NavLink to="/" className={({isActive}) => `transition-colors ${isActive ? 'text-primary' : 'hover:text-primary'}`}>{t('home')}</NavLink>
                            <NavLink to="/products" className={({isActive}) => `transition-colors ${isActive ? 'text-primary' : 'hover:text-primary'}`}>{t('products')}</NavLink>
                            <NavLink to="/services" className={({isActive}) => `transition-colors ${isActive ? 'text-primary' : 'hover:text-primary'}`}>Services</NavLink>
                        </nav>
                        <div className="hidden md:flex items-center space-x-4">
                            <div className="relative group">
                                <button className="flex items-center"><Languages/> <span className="ml-1 uppercase">{i18n.language}</span></button>
                                <div className="absolute right-0 mt-2 w-24 bg-card border border-border rounded-md shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-50">
                                    <button onClick={() => changeLanguage('en')} className="block w-full text-left px-4 py-2 text-sm hover:bg-secondary">English</button>
                                    <button onClick={() => changeLanguage('fr')} className="block w-full text-left px-4 py-2 text-sm hover:bg-secondary">Français</button>
                                    <button onClick={() => changeLanguage('ar')} className="block w-full text-left px-4 py-2 text-sm hover:bg-secondary">العربية</button>
                                </div>
                            </div>
                            <Link to="/cart" className="relative hover:text-primary transition-colors">
                                <ShoppingCart />
                                {cartCount > 0 && (
                                    <span className="absolute -top-2 -right-2 bg-primary text-primary-foreground text-xs rounded-full h-5 w-5 flex items-center justify-center">{cartCount}</span>
                                )}
                            </Link>
                            {currentUser ? (
                                <div className="relative group">
                                    <button className="flex items-center"><User /></button>
                                    <div className="absolute right-0 mt-2 w-48 bg-card border border-border rounded-md shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-50">
                                        <div className="px-4 py-2 text-sm text-gray-400 border-b border-border">Signed in as <br/> <span className="font-bold text-white">{appUser?.email}</span></div>
                                        <Link to="/account" className="block w-full text-left px-4 py-2 text-sm hover:bg-secondary">My Account</Link>
                                        {(appUser?.role === 'ADMIN' || appUser?.role === 'SELLER') && (
                                            <Link to="/admin/dashboard" className="block w-full text-left px-4 py-2 text-sm hover:bg-secondary">Dashboard</Link>
                                        )}
                                        <button onClick={handleLogout} className="flex items-center w-full text-left px-4 py-2 text-sm hover:bg-secondary text-red-400">
                                            <LogOut size={16} className="mr-2"/> Logout
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <Link to="/login" className="hover:text-primary transition-colors"><User /></Link>
                            )}
                        </div>
                        <div className="md:hidden">
                            <button onClick={() => setIsMenuOpen(!isMenuOpen)}>
                                {isMenuOpen ? <X/> : <Menu/>}
                            </button>
                        </div>
                    </div>
                </div>
                {isMenuOpen && (
                    <div className="md:hidden py-4 border-t border-border">
                        <nav className="flex flex-col space-y-4 px-4">
                            <NavLink to="/" onClick={() => setIsMenuOpen(false)}>Home</NavLink>
                            <NavLink to="/products" onClick={() => setIsMenuOpen(false)}>Products</NavLink>
                            <NavLink to="/services" onClick={() => setIsMenuOpen(false)}>Services</NavLink>
                            <Link to="/cart" onClick={() => setIsMenuOpen(false)}>Cart ({cartCount})</Link>
                            {currentUser ? (
                                <>
                                    <Link to="/account" onClick={() => setIsMenuOpen(false)}>My Account</Link>
                                    {(appUser?.role === 'ADMIN' || appUser?.role === 'SELLER') && (
                                        <Link to="/admin/dashboard" onClick={() => setIsMenuOpen(false)}>Dashboard</Link>
                                    )}
                                    <button onClick={handleLogout}>Logout</button>
                                </>
                            ) : (
                                <Link to="/login" onClick={() => setIsMenuOpen(false)}>Login</Link>
                            )}
                        </nav>
                    </div>
                )}
            </header>

            <main className="flex-1 container mx-auto px-4 py-8 animate-fade-in">
                {children}
            </main>

            <footer className="bg-card border-t border-border py-8">
                <div className="container mx-auto px-4 text-center text-gray-400">
                    <div className="flex justify-center space-x-6 mb-6">
                        {facebookUrl && <a href={facebookUrl} target="_blank" rel="noopener noreferrer" className="hover:text-white transition-colors"><Facebook/></a>}
                        {twitterUrl && <a href={twitterUrl} target="_blank" rel="noopener noreferrer" className="hover:text-white transition-colors"><Twitter/></a>}
                        {instagramUrl && <a href={instagramUrl} target="_blank" rel="noopener noreferrer" className="hover:text-white transition-colors"><Instagram/></a>}
                    </div>
                    <div className="flex justify-center space-x-6 mb-4">
                        <Link to="/page/terms-and-conditions" className="hover:text-white transition-colors">Terms & Conditions</Link>
                        <Link to="/page/privacy-policy" className="hover:text-white transition-colors">Privacy Policy</Link>
                    </div>
                    <p>© {new Date().getFullYear()} Media Tower. All Rights Reserved.</p>
                </div>
            </footer>

            <ChatBubble />
        </div>
    );
};

export default SiteLayout;