import http from "k6/http";
import { check, group, sleep } from "k6";

export const options = {
  stages: [
    { duration: "5s", target: 50 }, // Warm-up to 50 VUs
    { duration: "10s", target: 150 }, // Ramp to 150 VUs
    { duration: "15s", target: 200 }, // Peak sustained load at 200 VUs
    { duration: "5s", target: 0 }, // Cool-down
  ],
  thresholds: {
    // 95% of redirect requests must complete in under 100ms under peak 200-VU saturation (Mac Docker VM)
    "http_req_duration{type:redirect}": ["p(95)<100"],
    // Overall failure rate must remain below 1%
    http_req_failed: ["rate<0.01"],
  },
};

const BASE_URL = __ENV.TARGET_URL || "http://localhost:8080";

export function setup() {
  const shortCodes = [];

  // Seed 5 initial links across shards
  for (let i = 1; i <= 5; i++) {
    const payload = JSON.stringify({
      longUrl: `https://shortlink.production.io/page-${i}-${Date.now()}`,
    });
    const params = {
      headers: {
        "Content-Type": "application/json",
        "X-Forwarded-For": `192.168.1.${i}`,
      },
    };
    const res = http.post(`${BASE_URL}/api/v1/links`, payload, params);
    if (res.status === 201) {
      const data = JSON.parse(res.body);
      shortCodes.push(data.shortCode);
    }
  }

  return { shortCodes: shortCodes.length > 0 ? shortCodes : ["prom-test"] };
}

export default function (data) {
  const rand = Math.random();
  const clientIp = `172.16.${(__VU % 4) + 1}.${(__ITER % 250) + 1}`;

  if (rand < 0.85) {
    // 85% Traffic: High-speed Redirect Engine (L1/L2 Cache + Kafka click dispatch)
    const shortCode = data.shortCodes[__ITER % data.shortCodes.length];
    const res = http.get(`${BASE_URL}/${shortCode}`, {
      redirects: 0,
      tags: { type: "redirect" },
      headers: {
        "X-Forwarded-For": clientIp,
        "User-Agent":
          "k6-capstone-load-test/1.0 (Macintosh; Intel Mac OS X 10_15_7)",
      },
    });

    check(res, {
      "redirect status is 302": (r) => r.status === 302,
      "location header present": (r) => r.headers["Location"] !== undefined,
    });
  } else if (rand < 0.95) {
    // 10% Traffic: Real-Time Click Analytics & Time Series Queries
    const shortCode = data.shortCodes[__ITER % data.shortCodes.length];
    const res = http.get(
      `${BASE_URL}/api/v1/links/${shortCode}/analytics?window=24h`,
      {
        tags: { type: "analytics" },
        headers: {
          "X-Forwarded-For": clientIp,
        },
      },
    );

    check(res, {
      "analytics status is 200": (r) => r.status === 200,
    });
  } else {
    // 5% Traffic: Sharded Link Creation (Distributed Writes via Consistent Hash Ring)
    const payload = JSON.stringify({
      longUrl: `https://example.com/target-${__VU}-${__ITER}-${Date.now()}`,
    });
    const res = http.post(`${BASE_URL}/api/v1/links`, payload, {
      tags: { type: "create" },
      headers: {
        "Content-Type": "application/json",
        "X-Forwarded-For": clientIp,
      },
    });

    check(res, {
      "create status is 201": (r) => r.status === 201,
    });
  }
}
