type DateGroup<T> = {
  label: string;
  items: T[];
};

export function groupByDate<T>(
  items: T[],
  getDate: (item: T) => string
): DateGroup<T>[] {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  const sevenDaysAgo = new Date(today);
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

  const groups: Record<string, T[]> = {
    "Hoje": [],
    "Ontem": [],
    "Ultimos 7 dias": [],
    "Anteriores": []
  };

  for (const item of items) {
    const dateStr = getDate(item);
    const date = new Date(dateStr);

    if (date >= today) {
      groups["Hoje"].push(item);
    } else if (date >= yesterday) {
      groups["Ontem"].push(item);
    } else if (date >= sevenDaysAgo) {
      groups["Ultimos 7 dias"].push(item);
    } else {
      groups["Anteriores"].push(item);
    }
  }

  return Object.entries(groups)
    .filter(([, items]) => items.length > 0)
    .map(([label, items]) => ({ label, items }));
}
