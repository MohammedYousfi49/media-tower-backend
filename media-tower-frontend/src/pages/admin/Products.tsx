import { useEffect, useState, FormEvent, ChangeEvent } from 'react';
import { PlusCircle, Edit, Trash2, Loader2, Image as ImageIcon, FileText, XCircle } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import Modal from '../../components/shared/Modal';
import { AxiosError } from 'axios';

// Interfaces pour les données
interface Media {
    id: number;
    url: string;
    originalName: string;
    isPrimary: boolean;
    file?: File;
    isNew?: boolean;
    isDeleted?: boolean;
}
interface Product {
    id: number;
    names: { [key: string]: string };
    descriptions: { [key: string]: string };
    price: number;
    stock: number;
    categoryName: string;
    images: Media[];
    digitalAssets: Media[];
    tagIds: number[];
    categoryId: number;
}
interface Category { id: number; names: { [key: string]: string }; }
interface Tag { id: number; name: string; }
type SupportedLanguage = 'fr' | 'en';
const LANGUAGES: SupportedLanguage[] = ['fr', 'en'];

const Products = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [tags, setTags] = useState<Tag[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentProduct, setCurrentProduct] = useState<Partial<Product>>({});
    const [saving, setSaving] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [currentPage, setCurrentPage] = useState(1);
    const itemsPerPage = 10;
    const [loading, setLoading] = useState(true);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [prodRes, catRes, tagRes] = await Promise.all([
                axiosClient.get<Product[]>('/products'),
                axiosClient.get<Category[]>('/categories'),
                axiosClient.get<Tag[]>('/tags')
            ]);
            setProducts(prodRes.data);
            setCategories(catRes.data);
            setTags(tagRes.data);
        } catch (error) {
            console.error('Failed to fetch initial data:', error);
            alert('Failed to load data. Please try again later.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleOpenModal = (product: Partial<Product> | null = null) => {
        const initialProduct: Partial<Product> = {
            names: {fr: '', en: ''},
            descriptions: {fr: '', en: ''},
            price: 0,
            stock: 0,
            tagIds: [],
            images: [],
            digitalAssets: [],
            categoryId: categories[0]?.id || 0
        };

        setCurrentProduct({
            ...initialProduct,
            ...product,
            names: { ...initialProduct.names, ...product?.names },
            descriptions: { ...initialProduct.descriptions, ...product?.descriptions },
            images: product?.images ? [...product.images] : [],
            digitalAssets: product?.digitalAssets ? [...product.digitalAssets] : [],
            tagIds: product?.tagIds ? [...product.tagIds] : [],
        });
        setIsModalOpen(true);
    };

    const handleCloseModal = () => setIsModalOpen(false);

    const handleSave = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setSaving(true);
        const form = e.currentTarget;
        const selectedTagIds = tags.filter(t => (form[`tag-${t.id}`] as HTMLInputElement)?.checked).map(t => t.id);

        const existingImagesToKeep = currentProduct.images?.filter(img => !img.isDeleted && !img.isNew) || [];
        const existingDigitalAssetsToKeep = currentProduct.digitalAssets?.filter(asset => !asset.isDeleted && !asset.isNew) || [];

        const productDto = {
            names: { fr: (form.name_fr as HTMLInputElement).value || null, en: (form.name_en as HTMLInputElement).value || null },
            descriptions: { fr: (form.desc_fr as HTMLTextAreaElement).value || null, en: (form.desc_en as HTMLTextAreaElement).value || null },
            price: Number((form.price as HTMLInputElement).value),
            stock: Number((form.stock as HTMLInputElement).value),
            categoryId: Number((form.categoryId as HTMLSelectElement).value),
            tagIds: selectedTagIds,
            images: existingImagesToKeep.map(img => ({
                id: img.id,
                url: img.url,
                originalName: img.originalName,
                isPrimary: img.isPrimary
            })),
            digitalAssets: existingDigitalAssetsToKeep.map(asset => ({
                id: asset.id,
                url: asset.url,
                originalName: asset.originalName,
                isPrimary: asset.isPrimary
            })),
        };

        const formData = new FormData();
        formData.append("productDto", new Blob([JSON.stringify(productDto)], { type: "application/json" }));

        currentProduct.images?.filter(img => img.isNew && img.file).forEach(media => {
            if (media.file) formData.append("newImages", media.file);
        });
        currentProduct.digitalAssets?.filter(asset => asset.isNew && asset.file).forEach(media => {
            if (media.file) formData.append("newDigitalAssets", media.file);
        });

        try {
            // ===================================================================
            // --- DÉBUT DE LA CORRECTION ---
            // On supprime l'en-tête 'Content-Type' pour laisser le navigateur
            // le générer automatiquement avec la bonne "boundary".
            // ===================================================================
            if (currentProduct?.id) {
                await axiosClient.put(`/products/${currentProduct.id}`, formData);
            } else {
                await axiosClient.post('/products', formData);
            }
            // ===================================================================
            // --- FIN DE LA CORRECTION ---
            // ===================================================================

            fetchData();
            handleCloseModal();
        } catch (error) {
            console.error('Failed to save product', error);
            if (error instanceof AxiosError && error.response && error.response.status === 403) {
                alert('Access Denied: You do not have permission to perform this action. Please check your authentication and user roles.');
            } else {
                alert('Error saving product. Check console for details.');
            }
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this product? This action cannot be undone.')) {
            try {
                await axiosClient.delete(`/products/${id}`);
                setProducts(prevProducts => prevProducts.filter(p => p.id !== id));
                alert('Product deleted successfully!');
            } catch (error) {
                console.error('Failed to delete product', error);
                if (error instanceof AxiosError && error.response && error.response.status === 403) {
                    alert('Access Denied: You do not have permission to delete this product. Please check your authentication and user roles.');
                } else {
                    alert('Error deleting product. Check console for details.');
                }
            }
        }
    };

    const handleImageFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            const files = Array.from(e.target.files);
            const newMediaFiles: Media[] = files.map(file => ({
                id: Date.now() + Math.random(),
                url: URL.createObjectURL(file),
                originalName: file.name,
                isPrimary: false,
                file: file,
                isNew: true,
            }));

            setCurrentProduct(prev => {
                const currentImages = prev.images?.filter(img => !img.isDeleted) || [];
                const allImages = [...currentImages, ...newMediaFiles];
                if (allImages.length > 0 && !allImages.some(img => img.isPrimary)) {
                    allImages[0].isPrimary = true;
                }
                return { ...prev, images: allImages };
            });
        }
    };

    const handleDigitalAssetFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            const files = Array.from(e.target.files);
            const newMediaFiles: Media[] = files.map(file => ({
                id: Date.now() + Math.random(),
                url: '',
                originalName: file.name,
                isPrimary: false,
                file: file,
                isNew: true,
            }));

            setCurrentProduct(prev => ({
                ...prev,
                digitalAssets: [...(prev.digitalAssets?.filter(asset => !asset.isDeleted) || []), ...newMediaFiles]
            }));
        }
    };

    const handleSetPrimaryImage = (id: number) => {
        setCurrentProduct(prev => ({
            ...prev,
            images: prev.images?.map(img => ({
                ...img,
                isPrimary: img.id === id
            })) || []
        }));
    };

    const handleRemoveMedia = (id: number, type: 'image' | 'digitalAsset') => {
        setCurrentProduct(prev => {
            const mediaList = type === 'image' ? prev.images : prev.digitalAssets;
            const updatedList = mediaList?.map(media =>
                media.id === id ? { ...media, isDeleted: true } : media
            ).filter(media => !(media.isNew && media.isDeleted));

            if (type === 'image' && prev.images?.find(img => img.id === id)?.isPrimary) {
                const remainingImages = updatedList?.filter(img => !img.isDeleted);
                if (remainingImages && remainingImages.length > 0) {
                    remainingImages[0].isPrimary = true;
                }
            }

            return {
                ...prev,
                [type === 'image' ? 'images' : 'digitalAssets']: updatedList
            };
        });
    };

    const filteredProducts = products.filter(p =>
        p.names.en?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.names.fr?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.categoryName?.toLowerCase().includes(searchTerm.toLowerCase())
    );
    const paginatedProducts = filteredProducts.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);
    const totalPages = Math.ceil(filteredProducts.length / itemsPerPage);

    return (
        <div className="p-6 bg-background min-h-screen text-text-color">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-white">Manage Products</h1>
                <div className="flex items-center gap-4">
                    <input
                        value={searchTerm}
                        onChange={e => { setSearchTerm(e.target.value); setCurrentPage(1); }}
                        placeholder="Search by Name or Category..."
                        className="bg-card border border-border p-2 rounded-lg text-text-color focus:ring-primary focus:border-primary shadow-sm"
                    />
                    <button
                        onClick={() => handleOpenModal()}
                        className="bg-primary text-primary-foreground px-4 py-2 rounded-lg flex items-center hover:bg-primary/90 transition-colors shadow-md"
                    >
                        <PlusCircle className="mr-2" size={20} /> Add New
                    </button>
                </div>
            </div>

            <div className="bg-card p-4 rounded-lg border border-border overflow-x-auto shadow-lg">
                <table className="w-full text-left table-auto">
                    <thead>
                    <tr className="border-b border-border text-sm text-gray-400 uppercase">
                        <th className="p-3 w-16">Image</th>
                        <th className="p-3">Name (EN)</th>
                        <th className="p-3">Name (FR)</th>
                        <th className="p-3">Category</th>
                        <th className="p-3 w-28 text-right">Price (DH)</th>
                        <th className="p-3 w-20 text-right">Stock</th>
                        <th className="p-3 w-28 text-center">Files</th>
                        <th className="p-3 w-24 text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {loading ? (
                        <tr><td colSpan={8} className="text-center p-8"><Loader2 className="animate-spin inline-block mr-2 text-primary" size={24} /> Loading products...</td></tr>
                    ) : paginatedProducts.length === 0 ? (
                        <tr><td colSpan={8} className="text-center p-8 text-gray-500">No products found for your search criteria.</td></tr>
                    ) : (
                        paginatedProducts.map(product => {
                            const primaryImage = product.images?.find(img => img.isPrimary && !img.isDeleted) || product.images?.find(img => !img.isDeleted);
                            const otherImagesCount = (product.images?.filter(img => !img.isPrimary && !img.isDeleted).length || 0);
                            const digitalAssetsCount = (product.digitalAssets?.filter(asset => !asset.isDeleted).length || 0);

                            return (
                                <tr key={product.id} className="border-b border-border last:border-b-0 hover:bg-hover-card transition-colors">
                                    <td className="p-2">
                                        <img
                                            src={primaryImage?.url || 'https://via.placeholder.com/40x40?text=No+Img'}
                                            alt={product.names.en || 'Product Image'}
                                            className="w-12 h-12 rounded object-cover border border-gray-700"
                                            onError={(e) => { e.currentTarget.src = 'https://via.placeholder.com/40x40?text=Error'; }}
                                        />
                                        {otherImagesCount > 0 && (
                                            <span className="text-xs text-gray-500 block mt-1">
                                                    +{otherImagesCount} more
                                                </span>
                                        )}
                                    </td>
                                    <td className="p-3">{product.names.en || 'N/A'}</td>
                                    <td className="p-3">{product.names.fr || 'N/A'}</td>
                                    <td className="p-3">{product.categoryName || 'N/A'}</td>
                                    <td className="p-3 text-right">{product.price.toFixed(2)}</td>
                                    <td className="p-3 text-right">{product.stock}</td>
                                    <td className="p-3 text-center">
                                        {digitalAssetsCount > 0 ? (
                                            <span className="inline-flex items-center gap-1 bg-blue-700 text-white text-xs px-2 py-1 rounded-full">
                                                    <FileText size={14} /> {digitalAssetsCount}
                                                </span>
                                        ) : 'N/A'}
                                    </td>
                                    <td className="flex space-x-2 p-3 justify-center items-center h-full">
                                        <button
                                            onClick={() => handleOpenModal(product)}
                                            className="text-yellow-400 hover:text-yellow-300 transition-colors"
                                            title="Edit Product"
                                        >
                                            <Edit size={18} />
                                        </button>
                                        <button
                                            onClick={() => handleDelete(product.id)}
                                            className="text-red-500 hover:text-red-400 transition-colors"
                                            title="Delete Product"
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </td>
                                </tr>
                            );
                        })
                    )}
                    </tbody>
                </table>
                {totalPages > 1 && (
                    <div className="flex justify-center mt-6 space-x-2">
                        {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                            <button
                                key={page}
                                onClick={() => setCurrentPage(page)}
                                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors
                                    ${currentPage === page ? 'bg-primary text-primary-foreground shadow-md' : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'}
                                `}
                            >
                                {page}
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {isModalOpen && currentProduct && (
                <Modal isOpen={isModalOpen} onClose={handleCloseModal} title={currentProduct.id ? 'Edit Product' : 'Add New Product'}>
                    <form onSubmit={handleSave} className="space-y-6 max-h-[80vh] overflow-y-auto p-2
                        scrollbar-thin scrollbar-thumb-gray-600 scrollbar-track-gray-800 scrollbar-thumb-rounded-full scrollbar-track-rounded-full"
                    >
                        <div className="border border-border p-4 rounded-md bg-input-background shadow-sm">
                            <label className="block mb-3 font-semibold text-lg text-text-color">Product Images (for storefront)</label>
                            <input
                                name="images"
                                type="file"
                                multiple
                                accept="image/*"
                                onChange={handleImageFileChange}
                                className="block w-full text-sm text-gray-400 mb-4
                                    file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0
                                    file:text-sm file:font-semibold file:bg-violet-50 file:text-violet-700
                                    hover:file:bg-violet-100 cursor-pointer"
                            />
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                                {currentProduct.images?.filter(img => !img.isDeleted).map((img) => (
                                    <div key={img.id} className="relative group border border-gray-700 rounded-lg overflow-hidden shadow-sm aspect-w-16 aspect-h-9 flex items-center justify-center">
                                        <img src={img.url} alt={img.originalName} className="w-full h-full object-cover"/>
                                        <div className="absolute inset-0 bg-black bg-opacity-60 flex flex-col items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity p-2 text-center">
                                            <button
                                                type="button"
                                                onClick={() => handleRemoveMedia(img.id, 'image')}
                                                className="text-red-400 hover:text-red-300 absolute top-2 right-2 p-1 bg-gray-800 rounded-full bg-opacity-75 transition-colors"
                                                title="Remove image"
                                            >
                                                <XCircle size={20} />
                                            </button>
                                            <span className="text-white text-xs break-words leading-tight mt-6 px-1">{img.originalName}</span>
                                            <button
                                                type="button"
                                                onClick={() => handleSetPrimaryImage(img.id)}
                                                className={`mt-2 px-3 py-1 text-xs rounded-full font-semibold transition-colors
                                                    ${img.isPrimary ? 'bg-green-600 text-white' : 'bg-gray-300 text-gray-800 hover:bg-gray-200'}
                                                `}
                                            >
                                                {img.isPrimary ? 'Primary' : 'Set Primary'}
                                            </button>
                                            {img.isPrimary && (
                                                <span className="absolute top-2 left-2 px-2 py-0.5 bg-green-700 text-white text-xs rounded-full bg-opacity-75">
                                                    Main
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                ))}
                                {currentProduct.images?.filter(img => !img.isDeleted).length === 0 && (
                                    <div className="relative group border border-gray-700 border-dashed rounded-lg flex items-center justify-center aspect-w-16 aspect-h-9 text-gray-500">
                                        <ImageIcon size={40} />
                                        <span className="text-sm mt-2">No Image Selected</span>
                                    </div>
                                )}
                            </div>
                        </div>

                        {LANGUAGES.map(lang => (
                            <div key={lang} className="p-3 border border-border rounded-md bg-input-background shadow-sm">
                                <h3 className="font-bold mb-2 text-text-color">{lang.toUpperCase()} Name / Description</h3>
                                <input
                                    name={`name_${lang}`}
                                    defaultValue={currentProduct.names?.[lang] || ''}
                                    placeholder={`Product Name (${lang})`}
                                    className="w-full bg-background p-2 rounded border border-gray-700 text-text-color focus:ring-primary focus:border-primary placeholder-gray-500 mb-2"
                                    required
                                />
                                <textarea
                                    name={`desc_${lang}`}
                                    defaultValue={currentProduct.descriptions?.[lang] || ''}
                                    placeholder={`Product Description (${lang})`}
                                    className="w-full bg-background p-2 rounded mt-2 border border-gray-700 text-text-color focus:ring-primary focus:border-primary placeholder-gray-500"
                                    rows={3}
                                />
                            </div>
                        ))}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <input
                                name="price"
                                type="number"
                                step="0.01"
                                defaultValue={currentProduct.price || 0}
                                placeholder="Price"
                                className="w-full bg-background p-2 rounded border border-gray-700 text-text-color focus:ring-primary focus:border-primary placeholder-gray-500"
                                required
                            />
                            <input
                                name="stock"
                                type="number"
                                defaultValue={currentProduct.stock || 0}
                                placeholder="Stock"
                                className="w-full bg-background p-2 rounded border border-gray-700 text-text-color focus:ring-primary focus:border-primary placeholder-gray-500"
                                required
                            />
                        </div>
                        <select
                            name="categoryId"
                            defaultValue={currentProduct.categoryId || ''}
                            className="w-full bg-background p-2 rounded border border-gray-700 text-text-color focus:ring-primary focus:border-primary"
                            required
                        >
                            <option value="" disabled>Select a Category</option>
                            {categories.map(cat => (
                                <option key={cat.id} value={cat.id}>
                                    {cat.names.en || cat.names.fr || `Category ${cat.id}`}
                                </option>
                            ))}
                        </select>

                        <div className="border border-border p-4 rounded-md bg-input-background shadow-sm">
                            <label className="block mb-3 font-semibold text-lg text-text-color">Digital Files (Product Content)</label>
                            <input
                                name="digitalAssets"
                                type="file"
                                multiple
                                accept=".pdf,.zip,video/*,audio/*,application/vnd.rar,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation"
                                onChange={handleDigitalAssetFileChange}
                                className="block w-full text-sm text-gray-400 mb-4
                                    file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0
                                    file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700
                                    hover:file:bg-blue-100 cursor-pointer"
                            />
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                                {currentProduct.digitalAssets?.filter(asset => !asset.isDeleted).map(asset => (
                                    <div key={asset.id} className="relative group border border-gray-700 rounded-lg overflow-hidden shadow-sm flex flex-col items-center justify-center p-2 text-center aspect-square">
                                        <FileText size={48} className="text-blue-400 mb-2"/>
                                        <span className="text-sm text-text-color break-words leading-tight">{asset.originalName}</span>
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveMedia(asset.id, 'digitalAsset')}
                                            className="text-red-400 hover:text-red-300 absolute top-2 right-2 p-1 bg-gray-800 rounded-full bg-opacity-75 transition-colors"
                                            title="Remove file"
                                        >
                                            <XCircle size={20} />
                                        </button>
                                    </div>
                                ))}
                                {currentProduct.digitalAssets?.filter(asset => !asset.isDeleted).length === 0 && (
                                    <div className="relative group border border-gray-700 border-dashed rounded-lg flex items-center justify-center aspect-square text-gray-500">
                                        <FileText size={40} />
                                        <span className="text-sm mt-2">No Digital Files Selected</span>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="border border-border p-3 rounded-md bg-input-background shadow-sm">
                            <h3 className="font-bold mb-2 text-text-color">Tags</h3>
                            <div className="max-h-40 overflow-y-auto border border-border p-2 rounded bg-background
                                scrollbar-thin scrollbar-thumb-gray-600 scrollbar-track-gray-800 scrollbar-thumb-rounded-full scrollbar-track-rounded-full"
                            >
                                {tags.map(tag => (
                                    <label key={tag.id} className="flex items-center space-x-2 py-1 text-text-color cursor-pointer hover:bg-hover-card rounded px-2">
                                        <input
                                            type="checkbox"
                                            name={`tag-${tag.id}`}
                                            defaultChecked={currentProduct.tagIds?.includes(tag.id)}
                                            className="form-checkbox h-4 w-4 text-primary rounded border-gray-600 bg-gray-700 checked:bg-primary focus:ring-primary"
                                        />
                                        <span>{tag.name}</span>
                                    </label>
                                ))}
                            </div>
                        </div>

                        <div className="flex justify-end pt-4">
                            <button
                                type="submit"
                                disabled={saving}
                                className="bg-primary flex items-center px-6 py-2 rounded-lg text-primary-foreground font-semibold hover:bg-primary/90 transition-colors disabled:bg-gray-600 disabled:cursor-not-allowed shadow-md"
                            >
                                {saving && <Loader2 className="animate-spin mr-2"/>}
                                {saving ? 'Saving...' : 'Save Product'}
                            </button>
                        </div>
                    </form>
                </Modal>
            )}
        </div>
    );
};

export default Products;