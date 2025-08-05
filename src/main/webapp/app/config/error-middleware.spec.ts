import errorMiddleware from './error-middleware';

// Mock console.error
const mockConsoleError = jest.spyOn(console, 'error').mockImplementation(() => {});

// Mock global DEVELOPMENT constant
const originalDevelopment = global.DEVELOPMENT;

describe('Error Middleware', () => {
  let next: jest.Mock;
  let middleware: any;

  beforeEach(() => {
    next = jest.fn();
    middleware = errorMiddleware();
    mockConsoleError.mockClear();
    // Set DEVELOPMENT to true by default
    global.DEVELOPMENT = true;
  });

  afterEach(() => {
    // Restore original value
    global.DEVELOPMENT = originalDevelopment;
  });

  afterAll(() => {
    mockConsoleError.mockRestore();
  });

  it('should call next with action when no error', () => {
    const action = { type: 'TEST_ACTION', payload: { data: 'test' } };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).not.toHaveBeenCalled();
  });

  it('should not log error when not in development mode', () => {
    global.DEVELOPMENT = false;
    const action = {
      type: 'TEST_ACTION',
      error: { message: 'Test error' },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).not.toHaveBeenCalled();
  });

  it('should log error message when in development mode and action has error', () => {
    const action = {
      type: 'TEST_ACTION',
      error: { message: 'Test error message' },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Test error message".');
  });

  it('should log error message and actual cause when error has response data', () => {
    const action = {
      type: 'TEST_ACTION',
      error: {
        message: 'Test error message',
        response: {
          data: {
            message: 'Server error',
            fieldErrors: [
              {
                field: 'email',
                objectName: 'User',
                message: 'Email is invalid',
              },
              {
                field: 'password',
                objectName: 'User',
                message: 'Password is required',
              },
            ],
          },
        },
      },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Test error message".');

    // Check that the second console.error call contains the expected content
    const secondCall = mockConsoleError.mock.calls[1][0];
    expect(secondCall).toContain('Actual cause: Server error');
    expect(secondCall).toContain('field: email,  Object: User, message: Email is invalid');
    expect(secondCall).toContain('field: password,  Object: User, message: Password is required');
  });

  it('should handle error with response data but no fieldErrors', () => {
    const action = {
      type: 'TEST_ACTION',
      error: {
        message: 'Test error message',
        response: {
          data: {
            message: 'Simple server error',
          },
        },
      },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Test error message".');
    expect(mockConsoleError).toHaveBeenCalledWith('Actual cause: Simple server error');
  });

  it('should handle error with response data but no message property', () => {
    const action = {
      type: 'TEST_ACTION',
      error: {
        message: 'Test error message',
        response: {
          data: {
            fieldErrors: [
              {
                field: 'email',
                objectName: 'User',
                message: 'Email is invalid',
              },
            ],
          },
        },
      },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Test error message".');
    expect(mockConsoleError).toHaveBeenCalledWith('Actual cause: undefined\nfield: email,  Object: User, message: Email is invalid\n');
  });

  it('should handle error with empty fieldErrors array', () => {
    const action = {
      type: 'TEST_ACTION',
      error: {
        message: 'Test error message',
        response: {
          data: {
            message: 'Server error',
            fieldErrors: [],
          },
        },
      },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Test error message".');
    expect(mockConsoleError).toHaveBeenCalledWith('Actual cause: Server error');
  });

  it('should handle error with null response data', () => {
    const action = {
      type: 'TEST_ACTION',
      error: {
        message: 'Test error message',
        response: {
          data: null,
        },
      },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Test error message".');
    // When data is null, the second console.error is not called
    expect(mockConsoleError).toHaveBeenCalledTimes(1);
  });

  it('should handle error with undefined response data', () => {
    const action = {
      type: 'TEST_ACTION',
      error: {
        message: 'Test error message',
        response: {
          data: undefined,
        },
      },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Test error message".');
    // When data is undefined, the second console.error is not called
    expect(mockConsoleError).toHaveBeenCalledTimes(1);
  });

  it('should handle error with no response property', () => {
    const action = {
      type: 'TEST_ACTION',
      error: {
        message: 'Test error message',
      },
    };

    middleware(next)(action);

    expect(next).toHaveBeenCalledWith(action);
    expect(mockConsoleError).toHaveBeenCalledWith('TEST_ACTION caught at middleware with reason: "Test error message".');
    expect(mockConsoleError).toHaveBeenCalledTimes(1);
  });
});
