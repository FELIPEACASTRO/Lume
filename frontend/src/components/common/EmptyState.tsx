interface EmptyStateProps {
  title: string;
  description: string;
}

export default function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <div className="text-center py-12">
      <h3 className="text-lg font-medium text-dark-600">{title}</h3>
      <p className="mt-2 text-dark-400">{description}</p>
    </div>
  );
}
