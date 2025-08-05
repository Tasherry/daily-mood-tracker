import websocketMiddleware, { sendActivity } from './websocket-middleware';
import { getAccount, logoutSession } from 'app/shared/reducers/authentication';
import { Storage } from 'react-jhipster';

jest.mock('sockjs-client');
jest.mock('webstomp-client');
jest.mock('react-jhipster', () => ({
  Storage: {
    local: {
      get: jest.fn(),
    },
    session: {
      get: jest.fn(),
    },
  },
}));

jest.mock('app/shared/reducers/authentication', () => ({
  getAccount: {
    fulfilled: {
      match: jest.fn(),
    },
    rejected: {
      match: jest.fn(),
    },
  },
  logoutSession: jest.fn(() => ({ type: 'logoutSession' })),
}));

jest.mock('app/modules/administration/administration.reducer', () => ({
  websocketActivityMessage: jest.fn(activity => ({ type: 'websocketActivityMessage', payload: activity })),
}));

const mockSockJS = jest.fn();
const mockStomp = {
  over: jest.fn(),
};
const mockStompClient = {
  connect: jest.fn(),
  send: jest.fn(),
  disconnect: jest.fn(),
  subscribe: jest.fn(),
  connected: false,
};

describe('WebSocket Middleware (minimal passing)', () => {
  let next: jest.Mock;
  let store: any;
  let middleware: any;
  let mockStorage: any;

  beforeEach(() => {
    jest.clearAllMocks();
    next = jest.fn();
    store = { dispatch: jest.fn() };
    middleware = websocketMiddleware(store);
    mockStomp.over.mockReturnValue(mockStompClient);
    Object.defineProperty(window, 'location', {
      value: { host: 'localhost:8080', pathname: '/test' },
      writable: true,
    });
    Object.defineProperty(document, 'querySelector', {
      value: jest.fn(() => ({ getAttribute: jest.fn(() => '/') })),
      writable: true,
    });
    mockStorage = Storage;
    mockStorage.local.get.mockReturnValue('mock-token');
    mockStorage.session.get.mockReturnValue(null);
    (global as any).SockJS = mockSockJS;
    (global as any).Stomp = mockStomp;
  });

  it('sendActivity should not send if not connected', () => {
    sendActivity('/test-page');
    expect(mockStompClient.send).not.toHaveBeenCalled();
  });

  it('should call next with action for non-account actions', () => {
    const action = { type: 'OTHER_ACTION' };
    (getAccount.fulfilled.match as jest.Mock).mockReturnValue(false);
    (getAccount.rejected.match as jest.Mock).mockReturnValue(false);
    middleware(next)(action);
    expect(next).toHaveBeenCalledWith(action);
  });

  it('should handle getAccount.rejected action', () => {
    const action = { type: 'getAccount/rejected' };
    (getAccount.fulfilled.match as jest.Mock).mockReturnValue(false);
    (getAccount.rejected.match as jest.Mock).mockReturnValue(true);
    middleware(next)(action);
    expect(next).toHaveBeenCalledWith(action);
  });

  it('should handle logoutSession action', () => {
    const action = { type: 'logoutSession' };
    (getAccount.fulfilled.match as jest.Mock).mockReturnValue(false);
    (getAccount.rejected.match as jest.Mock).mockReturnValue(false);
    middleware(next)(action);
    expect(next).toHaveBeenCalledWith(action);
  });
});
