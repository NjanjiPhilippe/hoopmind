export const APP_CONSTANTS = {
  API_BASE_URL: '/api/v1',
  CURRENT_SEASON: '2024-2025',
  ITEMS_PER_PAGE: 20,
  DEFAULT_TEAM_LOGO: 'assets/images/default-team-logo.png',
  DEFAULT_PLAYER_PHOTO: 'assets/images/default-player.png',
};

export const ROUTES = {
  HOME: '/',
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  DASHBOARD: '/dashboard',
  TEAMS: '/teams',
  TEAM_DETAIL: '/teams/:id',
  PLAYERS: '/players',
  PLAYER_DETAIL: '/players/:id',
  MATCHES: '/matches',
  MATCH_DETAIL: '/matches/:id',
  PREDICTIONS: '/predictions',
  STATISTICS: '/statistics',
  ADMIN: '/admin',
};

export const POSITIONS = {
  PG: 'Meneur (PG)',
  SG: 'Arrière (SG)',
  SF: 'Ailier (SF)',
  PF: 'Ailier Fort (PF)',
  C: 'Pivot (C)',
};

export const CONFERENCES = {
  EASTERN: 'Conférence Est',
  WESTERN: 'Conférence Ouest',
};