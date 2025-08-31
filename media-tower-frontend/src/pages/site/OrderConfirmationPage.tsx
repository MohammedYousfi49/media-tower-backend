import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CheckCircle } from 'lucide-react';

const OrderConfirmationPage = () => {
    const { orderId } = useParams<{ orderId: string }>();
    const { t } = useTranslation();

    return (
        <div className="text-center py-20">
            <CheckCircle className="mx-auto h-24 w-24 text-green-500"/>
            <h1 className="mt-4 text-4xl font-bold text-white">{t('orderConfirmation')}</h1>
            <p className="mt-2 text-lg text-gray-300">{t('orderSuccess')}</p>
            <p className="mt-2 text-gray-400">{t('orderNumber')}: <span className="font-bold text-white">#{orderId}</span></p>
            <Link to="/" className="mt-8 inline-block bg-primary text-white font-bold px-6 py-3 rounded-lg">
                {t('backToHome')}
            </Link>
        </div>
    );
};

export default OrderConfirmationPage;