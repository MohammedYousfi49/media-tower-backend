import type { ReactNode } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
}

const Modal = ({ isOpen, onClose, title, children }: ModalProps) => {
  if (!isOpen) return null;

  return (
      // Le fond noir semi-transparent
      <div className="fixed inset-0 bg-black bg-opacity-75 z-50 flex justify-center items-center p-4">
        {/* Le conteneur de la modale */}
        <div className="bg-card rounded-lg shadow-xl w-full max-w-2xl border border-gray-700 flex flex-col max-h-[90vh]">
          {/* En-tête de la modale */}
          <div className="flex-shrink-0 flex justify-between items-center p-4 border-b border-gray-600">
            <h2 className="text-xl font-bold text-white">{title}</h2>
            <button onClick={onClose} className="text-gray-400 hover:text-white">
              <X size={24} />
            </button>
          </div>

          {/* --- LA CORRECTION PRINCIPALE EST ICI --- */}
          {/* Contenu principal de la modale, maintenant avec une barre de défilement si nécessaire */}
          <div className="p-6 overflow-y-auto">
            {children}
          </div>
        </div>
      </div>
  );
};

export default Modal;