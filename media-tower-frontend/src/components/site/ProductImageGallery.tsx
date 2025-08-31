// Fichier : src/components/site/ProductImageGallery.tsx (Version finale : Carrousel + Lightbox)

import { useState } from 'react';

// 1. Imports pour le Lightbox
import Lightbox from "yet-another-react-lightbox";
import Zoom from "yet-another-react-lightbox/plugins/zoom";
import "yet-another-react-lightbox/styles.css";

// 2. Imports pour le Carrousel (Swiper)
import { Swiper, SwiperSlide } from 'swiper/react';
import { FreeMode, Navigation, Thumbs } from 'swiper/modules';
import type { Swiper as SwiperCore } from 'swiper/types';
import 'swiper/css';
import 'swiper/css/free-mode';
import 'swiper/css/navigation';
import 'swiper/css/thumbs';

// 3. Import pour l'icône de zoom
import { ZoomIn } from 'lucide-react';

// Interfaces
interface Media {
    id: number;
    url: string;
}
interface ProductImageGalleryProps {
    images: Media[];
}

// Styles CSS pour le carrousel et l'icône
const styles = `
  .main-swiper .swiper-slide {
    position: relative; /* Nécessaire pour positionner l'icône de zoom */
  }
  .main-swiper .swiper-button-prev,
  .main-swiper .swiper-button-next {
    color: #ffffff;
    background-color: rgba(0, 0, 0, 0.3);
    border-radius: 50%;
    width: 44px;
    height: 44px;
    transition: background-color 0.2s;
  }
  .main-swiper .swiper-button-prev:hover,
  .main-swiper .swiper-button-next:hover {
    background-color: rgba(0, 0, 0, 0.5);
  }
  .main-swiper .swiper-button-prev:after,
  .main-swiper .swiper-button-next:after {
    font-size: 20px;
  }
  .thumbs-swiper .swiper-slide {
    opacity: 0.5;
    transition: opacity 300ms;
    cursor: pointer;
  }
  .thumbs-swiper .swiper-slide-thumb-active {
    opacity: 1;
    border: 2px solid #8A2BE2; /* Couleur primaire */
    border-radius: 8px;
  }
  .thumbs-swiper .swiper-slide img {
    border-radius: 6px;
  }
  .zoom-icon-container {
    position: absolute;
    top: 12px;
    right: 12px;
    z-index: 10;
    background-color: rgba(0, 0, 0, 0.4);
    color: white;
    border-radius: 50%;
    padding: 8px;
    cursor: pointer;
    transition: background-color 0.2s, transform 0.2s;
  }
  .zoom-icon-container:hover {
    background-color: rgba(0, 0, 0, 0.7);
    transform: scale(1.1);
  }
`;

const ProductImageGallery = ({ images }: ProductImageGalleryProps) => {
    const [thumbsSwiper, setThumbsSwiper] = useState<SwiperCore | null>(null);

    // États pour le Lightbox
    const [lightboxOpen, setLightboxOpen] = useState(false);
    const [lightboxIndex, setLightboxIndex] = useState(0);

    if (!images || images.length === 0) {
        return <img src="https://via.placeholder.com/600" alt="No image available" className="w-full rounded-lg shadow-lg" />;
    }

    // Préparation des données pour le Lightbox
    const slides = images.map(image => ({ src: image.url }));

    return (
        <>
            <style>{styles}</style>

            {/* Carrousel Principal */}
            <Swiper
                className="main-swiper rounded-lg mb-4 h-96"
                modules={[FreeMode, Navigation, Thumbs]}
                spaceBetween={10}
                navigation={true}
                thumbs={{ swiper: thumbsSwiper && !thumbsSwiper.destroyed ? thumbsSwiper : null }}
                // Au changement de slide, on met à jour l'index pour le lightbox
                onSlideChange={(swiper) => setLightboxIndex(swiper.activeIndex)}
            >
                {images.map((image, index) => (
                    <SwiperSlide key={`main-${image.id}`}>
                        <img src={image.url} alt="Image du produit" className="w-full h-full object-cover" />

                        {/* Icône de zoom sur chaque image */}
                        <div
                            className="zoom-icon-container"
                            onClick={() => {
                                // On s'assure que l'index est correct avant d'ouvrir
                                setLightboxIndex(index);
                                setLightboxOpen(true);
                            }}
                        >
                            <ZoomIn size={24} />
                        </div>

                    </SwiperSlide>
                ))}
            </Swiper>

            {/* Carrousel des Vignettes (seulement s'il y a plus d'une image) */}
            {images.length > 1 && (
                <Swiper
                    className="thumbs-swiper h-24"
                    onSwiper={setThumbsSwiper}
                    modules={[FreeMode, Thumbs]}
                    spaceBetween={10}
                    slidesPerView={4}
                    freeMode={true}
                    watchSlidesProgress={true}
                >
                    {images.map(image => (
                        <SwiperSlide key={`thumb-${image.id}`}>
                            <img src={image.url} alt="Vignette du produit" className="w-full h-full object-cover" />
                        </SwiperSlide>
                    ))}
                </Swiper>
            )}

            {/* Le composant Lightbox qui s'affichera en overlay */}
            <Lightbox
                open={lightboxOpen}
                close={() => setLightboxOpen(false)}
                slides={slides}
                index={lightboxIndex}
                plugins={[Zoom]} // Activation du plugin de zoom
                // Mise à jour de l'index si l'utilisateur navigue dans le lightbox
                on={{ view: ({ index: currentIndex }) => setLightboxIndex(currentIndex) }}
            />
        </>
    );
};

export default ProductImageGallery;