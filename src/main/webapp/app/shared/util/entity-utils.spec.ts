import { cleanEntity, mapIdList, overrideSortStateWithQueryParams, overridePaginationStateWithQueryParams } from './entity-utils';
import { IPaginationBaseState, ISortBaseState } from 'react-jhipster';

describe('Entity utils', () => {
  describe('cleanEntity', () => {
    it('should not remove fields with an id', () => {
      const entityA = {
        a: {
          id: 5,
        },
      };
      const entityB = {
        a: {
          id: '5',
        },
      };

      expect(cleanEntity({ ...entityA })).toEqual(entityA);
      expect(cleanEntity({ ...entityB })).toEqual(entityB);
    });

    it('should remove fields with an empty id', () => {
      const entity = {
        a: {
          id: '',
        },
      };

      expect(cleanEntity({ ...entity })).toEqual({});
    });

    it('should remove fields with id equal to -1', () => {
      const entity = {
        a: {
          id: -1,
        },
        b: {
          id: 1,
        },
      };

      expect(cleanEntity({ ...entity })).toEqual({
        b: {
          id: 1,
        },
      });
    });

    it('should not remove fields that are not objects', () => {
      const entity = {
        a: '',
        b: 5,
        c: [],
        d: '5',
      };

      expect(cleanEntity({ ...entity })).toEqual(entity);
    });

    it('should handle entity with mixed field types', () => {
      const entity = {
        id: 1,
        name: 'Test',
        category: {
          id: '',
        },
        tags: [
          { id: 1, name: 'tag1' },
          { id: '', name: 'tag2' },
        ],
        settings: {
          id: -1,
          theme: 'dark',
        },
        metadata: {
          id: 5,
          version: '1.0',
        },
      };

      const expected = {
        id: 1,
        name: 'Test',
        tags: [
          { id: 1, name: 'tag1' },
          { id: '', name: 'tag2' }, // Array elements are not filtered by cleanEntity
        ],
        metadata: {
          id: 5,
          version: '1.0',
        },
      };

      expect(cleanEntity({ ...entity })).toEqual(expected);
    });

    it('should handle empty entity', () => {
      const entity = {};

      expect(cleanEntity({ ...entity })).toEqual({});
    });

    it('should handle entity with null and undefined values', () => {
      const entity = {
        id: 1,
        name: null,
        description: undefined,
        category: {
          id: '',
        },
      };

      const expected = {
        id: 1,
        name: null,
        description: undefined,
      };

      expect(cleanEntity({ ...entity })).toEqual(expected);
    });

    it('should handle nested objects with empty ids', () => {
      const entity = {
        id: 1,
        parent: {
          id: '',
          children: [
            { id: 1, name: 'child1' },
            { id: '', name: 'child2' },
          ],
        },
      };

      const expected = {
        id: 1,
      };

      expect(cleanEntity({ ...entity })).toEqual(expected);
    });
  });

  describe('mapIdList', () => {
    it("should map ids no matter the element's type", () => {
      const ids = ['jhipster', '', 1, { key: 'value' }];

      expect(mapIdList(ids)).toEqual([{ id: 'jhipster' }, { id: 1 }, { id: { key: 'value' } }]);
    });

    it('should return an empty array', () => {
      const ids = [];

      expect(mapIdList(ids)).toEqual([]);
    });

    it('should filter out empty string ids', () => {
      const ids = ['', 'valid1', '', 'valid2', ''];

      expect(mapIdList(ids)).toEqual([{ id: 'valid1' }, { id: 'valid2' }]);
    });

    it('should handle array with only empty strings', () => {
      const ids = ['', '', ''];

      expect(mapIdList(ids)).toEqual([]);
    });

    it('should handle mixed types including null and undefined', () => {
      const ids = [null, undefined, '', 'valid', 0, false];

      expect(mapIdList(ids)).toEqual([{ id: null }, { id: undefined }, { id: 'valid' }, { id: 0 }, { id: false }]);
    });

    it('should handle complex objects', () => {
      const ids = [{ id: 1, name: 'object1' }, { id: 2, name: 'object2' }, '', { id: 3, name: 'object3' }];

      expect(mapIdList(ids)).toEqual([
        { id: { id: 1, name: 'object1' } },
        { id: { id: 2, name: 'object2' } },
        { id: { id: 3, name: 'object3' } },
      ]);
    });
  });

  describe('overrideSortStateWithQueryParams', () => {
    it('should override sort state with query parameters', () => {
      const sortState: ISortBaseState = {
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?sort=name,desc';

      const result = overrideSortStateWithQueryParams(sortState, locationSearch);

      expect(result.sort).toBe('name');
      expect(result.order).toBe('desc');
    });

    it('should not override sort state when no sort parameter', () => {
      const sortState: ISortBaseState = {
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?page=1&size=10';

      const result = overrideSortStateWithQueryParams(sortState, locationSearch);

      expect(result.sort).toBe('id');
      expect(result.order).toBe('asc');
    });

    it('should handle sort parameter without order', () => {
      const sortState: ISortBaseState = {
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?sort=name';

      const result = overrideSortStateWithQueryParams(sortState, locationSearch);

      expect(result.sort).toBe('name');
      expect(result.order).toBeUndefined();
    });

    it('should handle empty location search', () => {
      const sortState: ISortBaseState = {
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '';

      const result = overrideSortStateWithQueryParams(sortState, locationSearch);

      expect(result.sort).toBe('id');
      expect(result.order).toBe('asc');
    });

    it('should handle location search with only question mark', () => {
      const sortState: ISortBaseState = {
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?';

      const result = overrideSortStateWithQueryParams(sortState, locationSearch);

      expect(result.sort).toBe('id');
      expect(result.order).toBe('asc');
    });

    it('should handle multiple sort parameters (takes first)', () => {
      const sortState: ISortBaseState = {
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?sort=name,desc&sort=email,asc';

      const result = overrideSortStateWithQueryParams(sortState, locationSearch);

      expect(result.sort).toBe('name');
      expect(result.order).toBe('desc');
    });
  });

  describe('overridePaginationStateWithQueryParams', () => {
    it('should override pagination state with query parameters', () => {
      const paginationState: IPaginationBaseState = {
        activePage: 1,
        itemsPerPage: 10,
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?page=3&sort=name,desc';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(3);
      expect(result.sort).toBe('name');
      expect(result.order).toBe('desc');
      expect(result.itemsPerPage).toBe(10);
    });

    it('should not override page when no page parameter', () => {
      const paginationState: IPaginationBaseState = {
        activePage: 1,
        itemsPerPage: 10,
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?sort=name,desc';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(1);
      expect(result.sort).toBe('name');
      expect(result.order).toBe('desc');
    });

    it('should handle invalid page parameter', () => {
      const paginationState: IPaginationBaseState = {
        activePage: 1,
        itemsPerPage: 10,
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?page=invalid&sort=name,desc';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(NaN);
      expect(result.sort).toBe('name');
      expect(result.order).toBe('desc');
    });

    it('should handle zero page parameter', () => {
      const paginationState: IPaginationBaseState = {
        activePage: 1,
        itemsPerPage: 10,
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?page=0';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(0);
    });

    it('should handle negative page parameter', () => {
      const paginationState: IPaginationBaseState = {
        activePage: 1,
        itemsPerPage: 10,
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?page=-1';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(-1);
    });

    it('should handle empty location search', () => {
      const paginationState: IPaginationBaseState = {
        activePage: 1,
        itemsPerPage: 10,
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(1);
      expect(result.sort).toBe('id');
      expect(result.order).toBe('asc');
    });

    it('should handle complex query string', () => {
      const paginationState: IPaginationBaseState = {
        activePage: 1,
        itemsPerPage: 10,
        sort: 'id',
        order: 'asc',
      };
      const locationSearch = '?page=5&size=20&sort=createdDate,desc&filter=active&search=test';

      const result = overridePaginationStateWithQueryParams(paginationState, locationSearch);

      expect(result.activePage).toBe(5);
      expect(result.sort).toBe('createdDate');
      expect(result.order).toBe('desc');
      expect(result.itemsPerPage).toBe(10); // size parameter is not handled by this function
    });
  });
});
