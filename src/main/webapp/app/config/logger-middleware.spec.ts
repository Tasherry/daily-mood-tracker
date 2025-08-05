import loggerMiddleware from './logger-middleware';

// Mock console methods
const mockConsoleGroupCollapsed = jest.spyOn(console, 'groupCollapsed').mockImplementation(() => {});
const mockConsoleLog = jest.spyOn(console, 'log').mockImplementation(() => {});
const mockConsoleGroupEnd = jest.spyOn(console, 'groupEnd').mockImplementation(() => {});

// Mock global DEVELOPMENT constant
const originalDevelopment = global.DEVELOPMENT;

describe('Logger Middleware', () => {
  let next: jest.Mock;
  let middleware: any;

  beforeEach(() => {
    next = jest.fn();
    middleware = loggerMiddleware();
    mockConsoleGroupCollapsed.mockClear();
    mockConsoleLog.mockClear();
    mockConsoleGroupEnd.mockClear();
    // Set DEVELOPMENT to true by default
    global.DEVELOPMENT = true;
  });

  afterEach(() => {
    // Restore original value
    global.DEVELOPMENT = originalDevelopment;
  });

  afterAll(() => {
    mockConsoleGroupCollapsed.mockRestore();
    mockConsoleLog.mockRestore();
    mockConsoleGroupEnd.mockRestore();
  });

  it('should call next with action when not in development mode', () => {
    global.DEVELOPMENT = false;
    const action = { type: 'TEST_ACTION', payload: { data: 'test' } };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleGroupCollapsed).not.toHaveBeenCalled();
    expect(mockConsoleLog).not.toHaveBeenCalled();
    expect(mockConsoleGroupEnd).not.toHaveBeenCalled();
  });

  it('should log action details when in development mode', () => {
    const action = {
      type: 'TEST_ACTION',
      payload: { data: 'test payload' },
      meta: { timestamp: '2023-01-01' },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleGroupCollapsed).toHaveBeenCalledWith('TEST_ACTION');
    expect(mockConsoleLog).toHaveBeenCalledWith('Payload:', { data: 'test payload' });
    expect(mockConsoleLog).toHaveBeenCalledWith('Meta:', { timestamp: '2023-01-01' });
    expect(mockConsoleGroupEnd).toHaveBeenCalled();
  });

  it('should log action with error when in development mode', () => {
    const action = {
      type: 'TEST_ACTION',
      payload: { data: 'test payload' },
      meta: { timestamp: '2023-01-01' },
      error: { message: 'Test error' },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleGroupCollapsed).toHaveBeenCalledWith('TEST_ACTION');
    expect(mockConsoleLog).toHaveBeenCalledWith('Payload:', { data: 'test payload' });
    expect(mockConsoleLog).toHaveBeenCalledWith('Error:', { message: 'Test error' });
    expect(mockConsoleLog).toHaveBeenCalledWith('Meta:', { timestamp: '2023-01-01' });
    expect(mockConsoleGroupEnd).toHaveBeenCalled();
  });

  it('should log action with null/undefined values when in development mode', () => {
    const action = {
      type: 'TEST_ACTION',
      payload: null,
      meta: undefined,
      error: null,
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleGroupCollapsed).toHaveBeenCalledWith('TEST_ACTION');
    expect(mockConsoleLog).toHaveBeenCalledWith('Payload:', null);
    expect(mockConsoleLog).toHaveBeenCalledWith('Meta:', undefined);
    expect(mockConsoleGroupEnd).toHaveBeenCalled();
  });

  it('should log action with complex payload when in development mode', () => {
    const complexPayload = {
      user: {
        id: 1,
        name: 'John Doe',
        email: 'john@example.com',
      },
      settings: {
        theme: 'dark',
        notifications: true,
      },
    };
    const action = {
      type: 'USER_UPDATE',
      payload: complexPayload,
      meta: { source: 'form' },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleGroupCollapsed).toHaveBeenCalledWith('USER_UPDATE');
    expect(mockConsoleLog).toHaveBeenCalledWith('Payload:', complexPayload);
    expect(mockConsoleLog).toHaveBeenCalledWith('Meta:', { source: 'form' });
    expect(mockConsoleGroupEnd).toHaveBeenCalled();
  });

  it('should log action with complex error when in development mode', () => {
    const complexError = {
      message: 'Validation failed',
      code: 'VALIDATION_ERROR',
      details: {
        field: 'email',
        value: 'invalid-email',
        constraint: 'email',
      },
    };
    const action = {
      type: 'USER_CREATE',
      payload: { email: 'invalid-email' },
      error: complexError,
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleGroupCollapsed).toHaveBeenCalledWith('USER_CREATE');
    expect(mockConsoleLog).toHaveBeenCalledWith('Payload:', { email: 'invalid-email' });
    expect(mockConsoleLog).toHaveBeenCalledWith('Error:', complexError);
    expect(mockConsoleGroupEnd).toHaveBeenCalled();
  });

  it('should handle action with only type when in development mode', () => {
    const action = { type: 'SIMPLE_ACTION' };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleGroupCollapsed).toHaveBeenCalledWith('SIMPLE_ACTION');
    expect(mockConsoleLog).toHaveBeenCalledWith('Payload:', undefined);
    expect(mockConsoleLog).toHaveBeenCalledWith('Meta:', undefined);
    expect(mockConsoleGroupEnd).toHaveBeenCalled();
  });

  it('should call console methods in correct order', () => {
    const action = { type: 'TEST_ACTION', payload: { data: 'test' } };

    middleware(next)(action);

    // Verify that all console methods were called
    expect(mockConsoleGroupCollapsed).toHaveBeenCalled();
    expect(mockConsoleLog).toHaveBeenCalled();
    expect(mockConsoleGroupEnd).toHaveBeenCalled();
  });
});
