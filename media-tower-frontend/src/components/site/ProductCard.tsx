// Fichier : src/components/site/ProductCard.tsx

import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useCart } from '../../contexts/CartContext';
import { ShoppingCart } from 'lucide-react';

// Interface pour un seul objet média, tel qu'il vient du backend
interface Media {
  id: number;
  url: string;
  isPrimary: boolean;
}

// L'interface pour le produit a été mise à jour pour correspondre aux données du backend
interface ProductCardProps {
  product: {
    id: number;
    names: { [key: string]: string };
    price: number;
    images: Media[]; // On attend un tableau d'objets Media
    categoryName: string; // Gardé si vous l'utilisez
  };
}

const ProductCard = ({ product }: ProductCardProps) => {
  const { i18n } = useTranslation();
  const { addToCart } = useCart();

  const productName = product.names[i18n.language] || product.names.en || 'Unnamed Product';

  // --- Logique pour trouver l'URL de l'image ---
  // 1. Chercher l'image marquée comme "primaire"
  const primaryImage = product.images?.find(img => img.isPrimary);
  // 2. Si aucune image n'est primaire, ou si le tableau d'images existe mais est vide,
  //    on prend la première image de la liste.
  // 3. On définit l'URL à utiliser. Si aucune image n'est trouvée, on utilise une image par défaut.
  const imageUrl = primaryImage?.url || (product.images?.length > 0 ? product.images[0].url : 'https://via.placeholder.com/300');


  const handleAddToCart = () => {
    addToCart({
      id: product.id,
      name: productName,
      price: product.price,
      // On utilise la bonne URL d'image ici aussi
      imageUrl: imageUrl,
    });
  };

  return (
      <div className="bg-card rounded-lg overflow-hidden border border-gray-700 group flex flex-col
                    transform transition-transform duration-300 hover:scale-105 hover:shadow-lg hover:shadow-primary/20">

        <div className="overflow-hidden">
          <Link to={`/products/${product.id}`} className="block">
            <img
                // On utilise la variable imageUrl calculée
                src={imageUrl}
                alt={productName}
                className="w-full h-48 object-cover group-hover:scale-110 transition-transform duration-300"
            />
          </Link>
        </div>

        <div className="p-4 flex-grow flex flex-col">
          {/* Note: product.categoryName ne vient pas du DTO actuel. Il faudra l'ajouter au backend si besoin. */}
          <p className="text-sm text-gray-400">{product.categoryName || 'Uncategorized'}</p>
          <h3 className="text-lg font-bold text-white mt-1 flex-grow">
            <Link to={`/products/${product.id}`} className="hover:text-primary transition-colors">{productName}</Link>
          </h3>
          <div className="mt-4 flex justify-between items-center">
            <p className="text-xl font-semibold text-primary">{product.price.toFixed(2)} DH</p>
            <button onClick={handleAddToCart} className="bg-primary p-2 rounded-full text-white hover:bg-blue-600 transition-colors">
              <ShoppingCart size={20} />
            </button>
          </div>
        </div>
      </div>
  );
};

export default ProductCard;