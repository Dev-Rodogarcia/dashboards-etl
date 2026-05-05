/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

export interface PageHeaderMetadata {
  title: string;
  description?: string;
  updatedAt?: string | null;
}

interface PageHeaderContextValue {
  header: PageHeaderMetadata;
  setHeader: (metadata: PageHeaderMetadata) => void;
  resetHeader: () => void;
}

const DEFAULT_HEADER: PageHeaderMetadata = {
  title: 'Dashboards ETL',
  description: 'Painel de indicadores operacionais e logísticos.',
  updatedAt: null,
};

const PageHeaderContext = createContext<PageHeaderContextValue | null>(null);

export function PageHeaderProvider({ children }: { children: ReactNode }) {
  const [header, setHeaderState] = useState<PageHeaderMetadata>(DEFAULT_HEADER);

  const setHeader = useCallback((metadata: PageHeaderMetadata) => {
    setHeaderState({
      title: metadata.title,
      description: metadata.description,
      updatedAt: metadata.updatedAt ?? null,
    });
  }, []);

  const resetHeader = useCallback(() => {
    setHeaderState(DEFAULT_HEADER);
  }, []);

  const value = useMemo(
    () => ({
      header,
      setHeader,
      resetHeader,
    }),
    [header, resetHeader, setHeader],
  );

  return (
    <PageHeaderContext.Provider value={value}>
      {children}
    </PageHeaderContext.Provider>
  );
}

export function usePageHeader(metadata?: PageHeaderMetadata) {
  const context = useContext(PageHeaderContext);

  if (!context) {
    throw new Error('usePageHeader deve ser usado dentro de PageHeaderProvider.');
  }

  const metadataTitle = metadata?.title;
  const metadataDescription = metadata?.description;
  const metadataUpdatedAt = metadata?.updatedAt;
  const shouldRegister = Boolean(metadataTitle);
  const { header, resetHeader, setHeader } = context;

  useEffect(() => {
    if (!metadataTitle) return;
    setHeader({
      title: metadataTitle,
      description: metadataDescription,
      updatedAt: metadataUpdatedAt ?? null,
    });
  }, [metadataDescription, metadataTitle, metadataUpdatedAt, setHeader]);

  useEffect(() => {
    if (!shouldRegister) return;
    return resetHeader;
  }, [resetHeader, shouldRegister]);

  return header;
}
