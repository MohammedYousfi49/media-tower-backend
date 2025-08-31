// Fichier : src/pages/site/ProductListPage.tsx

import { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import ProductCard from '../../components/site/ProductCard';
import { useTranslation } from 'react-i18next';

// Interface pour un seul objet média
interface Media {
  id: number;
  url: string;
  isPrimary: boolean;
}

// L'interface Product est mise à jour pour correspondre aux données du backend
interface Product {
  id: number;
  names: { [key: string]: string };
  price: number;
  images: Media[]; // champ corrigé
  categoryId: number;
  categoryName: string; // champ optionnel, à ajouter au DTO backend si nécessaire
}

interface Category {
  id: number;
  names: { [key: string]: string };
}

const ProductListPage = () => {
  const { t, i18n } = useTranslation();
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [productsRes, categoriesRes] = await Promise.all([
          axiosClient.get('/products'),
          axiosClient.get('/categories')
        ]);
        setProducts(productsRes.data);
        setCategories(categoriesRes.data);
      } catch (error) {
        console.error("Failed to fetch data:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const filteredProducts = products.filter(product => {
    const nameInCurrentLang = product.names[i18n.language] || product.names.en;
    const matchesSearch = nameInCurrentLang.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = selectedCategory ? product.categoryId === parseInt(selectedCategory) : true;
    return matchesSearch && matchesCategory;
  });

  if (loading) return <div className="text-center">{t('loading')}</div>;

  return (
      <div>
        <h1 className="text-4xl font-bold mb-8 text-center">{t('products')}</h1>

        {/* Filtres */}
        <div className="flex flex-col md:flex-row gap-4 mb-8">
          <input
              type="text"
              placeholder="Search products..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="flex-grow bg-gray-700 text-white px-4 py-2 rounded-lg border border-gray-600"
          />
          <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="bg-gray-700 text-white px-4 py-2 rounded-lg border border-gray-600"
          >
            <option value="">{t('allCategories')}</option>
            {categories.map(cat => (
                <option key={cat.id} value={cat.id}>
                  {/* S'assurer que le DTO Category du backend a bien un champ 'names' */}
                  {cat.names ? (cat.names[i18n.language] || cat.names.en) : `Category ${cat.id}`}
                </option>
            ))}
          </select>
        </div>

        {/* Grille de produits */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredProducts.map(product => (
              <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </div>
  );
};

export default ProductListPage;