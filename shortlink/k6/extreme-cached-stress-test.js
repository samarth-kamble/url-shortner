import http from "k6/http";
import { check } from "k6";

export const options = {
  stages: [
    { duration: "5s", target: 50 }, // Warm-up to 50 VUs
    { duration: "10s", target: 150 }, // Ramp to 150 VUs
    { duration: "15s", target: 200 }, // Peak sustained load at 200 VUs (ZERO sleep)
    { duration: "5s", target: 0 }, // Graceful ramp-down
  ],
  thresholds: {
    // 95% of redirect requests must complete in under 50ms even at 200-VU peak saturation
    http_req_duration: ["p(95)<50"],
    // Less than 1% failure rate
    http_req_failed: ["rate<0.01"],
  },
};

const BASE_URL = __ENV.TARGET_URL || "http://localhost:8080";

export function setup() {
  const payload = JSON.stringify({
    longUrl: "https://github.com/samarth-kamble-7/shortlink",
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
      "X-Forwarded-For": "198.51.100.1",
    },
  };

  const res = http.post(`${BASE_URL}/api/v1/links`, payload, params);
  check(res, {
    "setup link created (201)": (r) => r.status === 201,
  });

  const body = JSON.parse(res.body);
  return { shortCode: body.shortCode };
}

export default function (data) {
  // Simulate 500 distinct distributed client IPs so RateLimitFilter checks real Redis buckets
  // without artificially tripping a single-IP burst limit inside k6
  const clientIp = `10.0.${(__VU % 2) + 1}.${(__ITER % 250) + 1}`;

  const res = http.get(`${BASE_URL}/${data.shortCode}`, {
    redirects: 0,
    headers: {
      "X-Forwarded-For": clientIp,
    },
  });

  check(res, {
    "status is 302 Found": (r) => r.status === 302,
    "has Location header": (r) => r.headers["Location"] !== undefined,
  });
}
