import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:6299';

export const options = {
  setupTimeout: '2m',
  scenarios: {
    map_current_reads: {
      executor: 'constant-vus',
      exec: 'mapCurrent',
      vus: 5,
      duration: '30s',
    },
    node_search_reads: {
      executor: 'constant-vus',
      exec: 'nodeSearch',
      vus: 3,
      startTime: '5s',
      duration: '30s',
    },
    node_create_writes: {
      executor: 'constant-vus',
      exec: 'nodeCreate',
      vus: 1,
      startTime: '10s',
      duration: '20s',
    },
  },
  thresholds: {
    'http_req_failed{scenario:map_current_reads}': ['rate<0.01'],
    'http_req_duration{scenario:map_current_reads}': ['p(95)<500'],
    'http_req_failed{scenario:node_search_reads}': ['rate<0.01'],
    'http_req_duration{scenario:node_search_reads}': ['p(95)<800'],
    'http_req_failed{scenario:node_create_writes}': ['rate<0.01'],
    'http_req_duration{scenario:node_create_writes}': ['p(95)<1000'],
  },
};

function jsonHeaders(extraTags = {}) {
  return {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: extraTags,
  };
}

function parseJson(response, label) {
  try {
    return response.json();
  } catch (error) {
    fail(`${label} returned non-JSON response: ${error}`);
  }
}

export function setup() {
  const currentMapResponse = http.get(
    `${BASE_URL}/api/map/current`,
    jsonHeaders({ endpoint: 'map_current_setup' }),
  );

  check(currentMapResponse, {
    'setup current map status is 200': (r) => r.status === 200,
  }) || fail(`Failed to read setup map: ${currentMapResponse.status} ${currentMapResponse.body}`);

  const currentMap = parseJson(currentMapResponse, 'setup current map');
  const rootId = currentMap.root && currentMap.root.id;
  if (!rootId) {
    fail(`Setup map did not include a root node id: ${currentMapResponse.body}`);
  }

  return {
    mapId: currentMap.mapId,
    rootId,
    searchQuery: currentMap.title || currentMap.root.text || '导图',
  };
}

export function mapCurrent() {
  const response = http.get(
    `${BASE_URL}/api/map/current`,
    jsonHeaders({ endpoint: 'map_current' }),
  );

  check(response, {
    'map current status is 200': (r) => r.status === 200,
    'map current contains root': (r) => {
      const body = parseJson(r, 'map current');
      return !!(body.root && body.root.id);
    },
  });

  sleep(1);
}

export function nodeSearch(data) {
  const response = http.post(
    `${BASE_URL}/api/nodes/search`,
    JSON.stringify({
      mapId: data.mapId,
      query: data.searchQuery,
      caseSensitive: false,
    }),
    jsonHeaders({ endpoint: 'nodes_search' }),
  );

  check(response, {
    'node search status is 200': (r) => r.status === 200,
    'node search has results array': (r) => Array.isArray(parseJson(r, 'node search').results),
  });

  sleep(1);
}

export function nodeCreate(data) {
  const response = http.post(
    `${BASE_URL}/api/nodes/create`,
    JSON.stringify({
      mapId: data.mapId,
      parentId: data.rootId,
      text: `k6-child-${__VU}-${__ITER}`,
    }),
    jsonHeaders({ endpoint: 'nodes_create' }),
  );

  check(response, {
    'node create status is 200': (r) => r.status === 200,
    'node create returns nodeId': (r) => !!parseJson(r, 'node create').nodeId,
  });

  sleep(1);
}
