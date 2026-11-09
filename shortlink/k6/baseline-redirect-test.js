import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '5s', target: 20 },   // Warm-up ramp to 20 VUs
        { duration: '15s', target: 50 },  // Sustained load at 50 VUs
        { duration: '5s', target: 0 },    // Ramp-down to 0
    ],
    thresholds: {
        // 95% of redirect requests must complete in under 50ms
        http_req_duration: ['p(95)<50'],
        // Less than 1% failure rate
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

export function setup() {
    const payload = JSON.stringify({
        longUrl: 'https://github.com/samarth-kamble-7/shortlink',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(`${BASE_URL}/api/v1/links`, payload, params);
    check(res, {
        'setup link created (201)': (r) => r.status === 201,
    });

    const body = JSON.parse(res.body);
    return { shortCode: body.shortCode };
}

export default function (data) {
    // We set redirects: 0 so k6 inspects our server's direct 302 Found response
    // without following to external GitHub servers
    const res = http.get(`${BASE_URL}/${data.shortCode}`, {
        redirects: 0,
    });

    check(res, {
        'status is 302 Found': (r) => r.status === 302,
        'has Location header': (r) => r.headers['Location'] !== undefined,
    });

    sleep(0.01); // 10ms pacing
}

