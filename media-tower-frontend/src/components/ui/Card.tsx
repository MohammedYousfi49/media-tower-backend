import type { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  className?: string;
}

const Card = ({ children, className = '' }: CardProps) => {
  return (
      <div className={`bg-card p-6 rounded-lg border border-gray-700 ${className}`}>
        {children}
      </div>
  );
};

export default Card;