import { AxiosError } from 'axios';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LoginPage from './LoginPage';

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
}));

vi.mock('../contexts/AutenticacaoContext', () => ({
  useAutenticacao: () => ({
    usuario: null,
    carregandoSessao: false,
    concluirTrocaSenhaObrigatoria: vi.fn(),
    login: mocks.login,
  }),
}));

vi.mock('next-themes', () => ({
  useTheme: () => ({ theme: 'dark', setTheme: vi.fn() }),
}));

describe('LoginPage', () => {
  beforeEach(() => {
    mocks.login.mockReset();
  });

  it('exibe a troca de senha obrigatoria ao receber o contrato 428', async () => {
    mocks.login.mockRejectedValue(new AxiosError(
      'Precondition Required',
      undefined,
      undefined,
      undefined,
      { status: 428, data: { status: 'PASSWORD_RESET_REQUIRED', userId: '51' } } as never,
    ));

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/auth/trocar-senha" element={<LoginPage />} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText('E-mail'), {
      target: { value: 'usuario@rodogarcia.com.br' },
    });
    fireEvent.change(screen.getByLabelText('Senha'), {
      target: { value: 'SenhaTemporaria@12' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Acessar plataforma' }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Defina sua nova senha' })).toBeTruthy();
    });
  });
});
// @vitest-environment jsdom
