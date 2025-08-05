import { isRejectedAction, isPendingAction, isFulfilledAction, serializeAxiosError, createEntitySlice, EntityState } from './reducer.utils';
import { AxiosError } from 'axios';

describe('Reducer Utils', () => {
  describe('isRejectedAction', () => {
    it('should return true for rejected action', () => {
      const action = { type: 'test/rejected' };
      expect(isRejectedAction(action)).toBe(true);
    });

    it('should return false for non-rejected action', () => {
      const action = { type: 'test/fulfilled' };
      expect(isRejectedAction(action)).toBe(false);
    });

    it('should return false for pending action', () => {
      const action = { type: 'test/pending' };
      expect(isRejectedAction(action)).toBe(false);
    });

    it('should return false for regular action', () => {
      const action = { type: 'test' };
      expect(isRejectedAction(action)).toBe(false);
    });
  });

  describe('isPendingAction', () => {
    it('should return true for pending action', () => {
      const action = { type: 'test/pending' };
      expect(isPendingAction(action)).toBe(true);
    });

    it('should return false for non-pending action', () => {
      const action = { type: 'test/fulfilled' };
      expect(isPendingAction(action)).toBe(false);
    });

    it('should return false for rejected action', () => {
      const action = { type: 'test/rejected' };
      expect(isPendingAction(action)).toBe(false);
    });

    it('should return false for regular action', () => {
      const action = { type: 'test' };
      expect(isPendingAction(action)).toBe(false);
    });
  });

  describe('isFulfilledAction', () => {
    it('should return true for fulfilled action', () => {
      const action = { type: 'test/fulfilled' };
      expect(isFulfilledAction(action)).toBe(true);
    });

    it('should return false for non-fulfilled action', () => {
      const action = { type: 'test/pending' };
      expect(isFulfilledAction(action)).toBe(false);
    });

    it('should return false for rejected action', () => {
      const action = { type: 'test/rejected' };
      expect(isFulfilledAction(action)).toBe(false);
    });

    it('should return false for regular action', () => {
      const action = { type: 'test' };
      expect(isFulfilledAction(action)).toBe(false);
    });
  });

  describe('serializeAxiosError', () => {
    it('should return axios error as is', () => {
      const axiosError = {
        isAxiosError: true,
        message: 'Network error',
        response: { status: 500 },
      } as AxiosError;

      const result = serializeAxiosError(axiosError);
      expect(result).toBe(axiosError);
    });

    it('should serialize error object with string properties', () => {
      const error = {
        name: 'ValidationError',
        message: 'Invalid input',
        stack: 'Error stack trace',
        code: 'VALIDATION_ERROR',
      };

      const result = serializeAxiosError(error);
      expect(result).toEqual({
        name: 'ValidationError',
        message: 'Invalid input',
        stack: 'Error stack trace',
        code: 'VALIDATION_ERROR',
      });
    });

    it('should serialize error object with mixed property types', () => {
      const error = {
        name: 'ValidationError',
        message: 'Invalid input',
        stack: 'Error stack trace',
        code: 400, // number, should be ignored
        details: { field: 'email' }, // object, should be ignored
      };

      const result = serializeAxiosError(error);
      expect(result).toEqual({
        name: 'ValidationError',
        message: 'Invalid input',
        stack: 'Error stack trace',
      });
    });

    it('should serialize error object with missing properties', () => {
      const error = {
        message: 'Simple error',
      };

      const result = serializeAxiosError(error);
      expect(result).toEqual({
        message: 'Simple error',
      });
    });

    it('should handle null error', () => {
      const result = serializeAxiosError(null);
      expect(result).toEqual({ message: 'null' });
    });

    it('should handle undefined error', () => {
      const result = serializeAxiosError(undefined);
      expect(result).toEqual({ message: 'undefined' });
    });

    it('should handle string error', () => {
      const result = serializeAxiosError('String error');
      expect(result).toEqual({ message: 'String error' });
    });

    it('should handle number error', () => {
      const result = serializeAxiosError(500);
      expect(result).toEqual({ message: '500' });
    });

    it('should handle boolean error', () => {
      const result = serializeAxiosError(false);
      expect(result).toEqual({ message: 'false' });
    });

    it('should handle empty object', () => {
      const result = serializeAxiosError({});
      expect(result).toEqual({});
    });

    it('should handle object with no string properties', () => {
      const error = {
        code: 400,
        details: { field: 'email' },
        timestamp: new Date(),
      };

      const result = serializeAxiosError(error);
      expect(result).toEqual({});
    });
  });

  describe('createEntitySlice', () => {
    interface TestEntity {
      id: number;
      name: string;
    }

    const initialState: EntityState<TestEntity> = {
      loading: false,
      errorMessage: null,
      entities: [],
      entity: {} as TestEntity,
      updating: false,
      updateSuccess: false,
    };

    it('should create slice with default reducers', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        extraReducers() {}, // Provide empty function
      });

      expect(slice.name).toBe('test');
      expect(slice.getInitialState()).toEqual(initialState);
    });

    it('should create slice with custom reducers', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        extraReducers() {}, // Provide empty function
        reducers: {
          customAction(state) {
            state.loading = true;
          },
        },
      });

      expect(slice.actions.customAction).toBeDefined();
    });

    it('should create slice with extra reducers', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        extraReducers(builder) {
          builder.addCase('test/loading', state => {
            state.loading = true;
          });
        },
      });

      expect(slice.name).toBe('test');
    });

    it('should include reset reducer by default', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        extraReducers() {}, // Provide empty function
      });

      expect(slice.actions.reset).toBeDefined();

      const state = { ...initialState, loading: true, errorMessage: 'error' };
      const newState = slice.reducer(state, slice.actions.reset());
      expect(newState).toEqual(initialState);
    });

    it('should handle rejection actions by default', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        extraReducers() {}, // Provide empty function
      });

      const state = { ...initialState, loading: true, updating: true, updateSuccess: true };
      const action = { type: 'test/rejected', error: { message: 'Error' } };
      const newState = slice.reducer(state, action);

      expect(newState.loading).toBe(false);
      expect(newState.updating).toBe(false);
      expect(newState.updateSuccess).toBe(false);
      expect(newState.errorMessage).toBe(null);
    });

    it('should skip rejection handling when specified', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        skipRejectionHandling: true,
        extraReducers() {}, // Provide empty function
      });

      const state = { ...initialState, loading: true, updating: true, updateSuccess: true };
      const action = { type: 'test/rejected', error: { message: 'Error' } };
      const newState = slice.reducer(state, action);

      // State should remain unchanged since rejection handling is skipped
      expect(newState.loading).toBe(true);
      expect(newState.updating).toBe(true);
      expect(newState.updateSuccess).toBe(true);
    });

    it('should handle custom rejection logic in extra reducers', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        skipRejectionHandling: true,
        extraReducers(builder) {
          builder.addMatcher(isRejectedAction, (state, action) => {
            state.loading = false;
            state.errorMessage = action.error?.message || 'Unknown error';
          });
        },
      });

      const state = { ...initialState, loading: true };
      const action = { type: 'test/rejected', error: { message: 'Custom error' } };
      const newState = slice.reducer(state, action);

      expect(newState.loading).toBe(false);
      expect(newState.errorMessage).toBe('Custom error');
    });

    it('should handle complex entity state', () => {
      const complexInitialState: EntityState<TestEntity> = {
        loading: false,
        errorMessage: null,
        entities: [
          { id: 1, name: 'Entity 1' },
          { id: 2, name: 'Entity 2' },
        ],
        entity: { id: 1, name: 'Entity 1' },
        updating: false,
        updateSuccess: false,
        totalItems: 2,
        links: { next: 'next-page' },
      };

      const slice = createEntitySlice({
        name: 'test',
        initialState: complexInitialState,
        extraReducers() {}, // Provide empty function
      });

      expect(slice.getInitialState()).toEqual(complexInitialState);
    });

    it('should handle action with no error property', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        extraReducers() {}, // Provide empty function
      });

      const state = { ...initialState, loading: true };
      const action = { type: 'test/rejected' }; // No error property
      const newState = slice.reducer(state, action);

      expect(newState.loading).toBe(false);
      expect(newState.errorMessage).toBe(null);
    });
  });
});
