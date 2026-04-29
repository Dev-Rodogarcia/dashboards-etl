export const PASSWORD_POLICY_HINT = 'Use pelo menos 12 caracteres com maiúscula, minúscula, número e caractere especial.';

export function getPasswordPolicyErrors(password: string): string[] {
  const senha = password ?? '';
  const errors: string[] = [];

  if (senha.length < 12) {
    errors.push('Use pelo menos 12 caracteres.');
  }
  if (!/[A-Z]/.test(senha)) {
    errors.push('Inclua ao menos uma letra maiúscula.');
  }
  if (!/[a-z]/.test(senha)) {
    errors.push('Inclua ao menos uma letra minúscula.');
  }
  if (!/\d/.test(senha)) {
    errors.push('Inclua ao menos um número.');
  }
  if (!/[^A-Za-z0-9\s]/.test(senha)) {
    errors.push('Inclua ao menos um caractere especial.');
  }

  return errors;
}

export function isPasswordPolicySatisfied(password: string): boolean {
  return getPasswordPolicyErrors(password).length === 0;
}
