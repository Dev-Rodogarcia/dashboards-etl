import type { ComponentType } from 'react';
import type { NavItem } from '../utils/accessControl';

export type HomeDashboardCategory = 'Operação' | 'Financeiro' | 'Comercial' | 'Executivo' | 'TI/ETL';
export type HomeDashboardFilter = HomeDashboardCategory | 'Todos' | 'Favoritos';
export type HomeNoticeTag = 'NOVO' | 'ATENCAO' | 'FIXADO';
export type HomeCommunicationTab = 'avisos' | 'atualizacoes' | 'pendencias';
export type HomeCommunicationPriority = 'Alta' | 'Média' | 'Baixa';

export interface HomeDashboardMeta {
  category: HomeDashboardCategory;
  description: string;
  keywords: string[];
  Icon: ComponentType<{ size?: number; className?: string }>;
  accent: string;
  priority: number;
}

export interface HomeDashboardItem extends Omit<NavItem, 'description'>, HomeDashboardMeta {
  isAccessible: boolean;
}

export interface HomeNotice {
  id: string;
  title: string;
  body: string;
  tag: HomeNoticeTag;
  audience: string;
  date: string;
  publishedAt?: string | null;
  updatedBy?: string | null;
}

export interface HomeNoticeApi {
  id: string;
  titulo: string;
  corpo: string;
  tag: HomeNoticeTag;
  publicoAlvo: string;
  publicadoEm: string;
  atualizadoPor?: string | null;
}

export interface HomeNoticePayload {
  titulo: string;
  corpo: string;
  tag: HomeNoticeTag;
  publicoAlvo: string;
}

export interface HomeNoticeFormState {
  title: string;
  body: string;
  tag: HomeNoticeTag;
  audience: string;
}

export interface HomeMetric {
  id: string;
  label: string;
  value: string;
  helper: string;
}
