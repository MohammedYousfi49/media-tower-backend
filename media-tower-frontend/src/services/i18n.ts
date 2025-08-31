import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// Nos fichiers de traduction
const resources = {
  en: {
    translation: {
      "welcome": "Welcome to Media Tower",
      "products": "Products",
      "home": "Home",
      "addToCart": "Add to Cart",
      "loading": "Loading...",
      "price": "Price",
      "category": "Category",
      "allCategories": "All Categories",
      "reviews": "Reviews",
      "submitReview": "Submit Review",
      "loginToReview": "You must be logged in to submit a review.",
      "rating": "Rating",
      "comment": "Comment",
      "shoppingCart": "Shopping Cart",
      "proceedToCheckout": "Proceed to Checkout",
      "total": "Total",
      "emptyCart": "Your cart is empty.",
      "checkout": "Checkout",
      "placeOrder": "Place Order",
      "loginToCheckout": "You must login to place an order.",
      "orderConfirmation": "Order Confirmation",
      "orderSuccess": "Your order has been placed successfully!",
      "orderNumber": "Order Number",
      "backToHome": "Back to Home",
    }
  },
  fr: {
    translation: {
      "welcome": "Bienvenue chez Media Tower",
      "products": "Produits",
      "home": "Accueil",
      "addToCart": "Ajouter au Panier",
      "loading": "Chargement...",
      "price": "Prix",
      "category": "Catégorie",
      "allCategories": "Toutes les catégories",
      "reviews": "Avis",
      "submitReview": "Soumettre un avis",
      "loginToReview": "Vous devez être connecté pour laisser un avis.",
      "rating": "Note",
      "comment": "Commentaire",
      "shoppingCart": "Panier d'achats",
      "proceedToCheckout": "Passer à la caisse",
      "total": "Total",
      "emptyCart": "Votre panier est vide.",
      "checkout": "Paiement",
      "placeOrder": "Passer la commande",
      "loginToCheckout": "Vous devez vous connecter pour passer une commande.",
      "orderConfirmation": "Confirmation de commande",
      "orderSuccess": "Votre commande a été passée avec succès !",
      "orderNumber": "Numéro de commande",
      "backToHome": "Retour à l'accueil",
    }
  },
  ar: {
    translation: {
      "welcome": "مرحبًا بكم في ميديا تاور",
      "products": "المنتجات",
      "home": "الرئيسية",
      "addToCart": "أضف إلى السلة",
      "loading": "جاري التحميل...",
      "price": "السعر",
      "category": "الفئة",
      "allCategories": "جميع الفئات",
      "reviews": "التقييمات",
      "submitReview": "إرسال تقييم",
      "loginToReview": "يجب عليك تسجيل الدخول لتقديم تقييم.",
      "rating": "التقييم",
      "comment": "تعليق",
      "shoppingCart": "سلة التسوق",
      "proceedToCheckout": "الانتقال إلى الدفع",
      "total": "المجموع",
      "emptyCart": "سلتك فارغة.",
      "checkout": "الدفع",
      "placeOrder": "إتمام الطلب",
      "loginToCheckout": "يجب عليك تسجيل الدخول لإتمام الطلب.",
      "orderConfirmation": "تأكيد الطلب",
      "orderSuccess": "لقد تم تقديم طلبك بنجاح!",
      "orderNumber": "رقم الطلب",
      "backToHome": "العودة إلى الرئيسية",
    }
  }
};

i18n
    .use(LanguageDetector) // Détecte la langue du navigateur
    .use(initReactI18next) // Lie react-i18next avec i18next
    .init({
      resources,
      fallbackLng: 'en', // Langue par défaut
      interpolation: {
        escapeValue: false // React s'en charge déjà
      },
      detection: {
        order: ['localStorage', 'navigator'],
        caches: ['localStorage'],
      },
    });

export default i18n;