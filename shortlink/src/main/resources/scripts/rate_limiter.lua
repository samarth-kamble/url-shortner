-- Atomic Token Bucket Rate Limiter
-- KEYS[1] = bucket key
-- ARGV[1] = capacity (max tokens)
-- ARGV[2] = refill_rate (tokens per second)
-- ARGV[3] = now_ms (current timestamp in milliseconds)
-- ARGV[4] = requested (tokens requested, typically 1)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now_ms = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

if tokens == nil or last_refill == nil then
    tokens = capacity
    last_refill = now_ms
else
    local elapsed_seconds = math.max(0, (now_ms - last_refill) / 1000.0)
    local added_tokens = elapsed_seconds * refill_rate
    tokens = math.min(capacity, tokens + added_tokens)
    last_refill = now_ms
end

local allowed = 0
local retry_after = 0

if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
else
    local missing = requested - tokens
    if refill_rate > 0 then
        retry_after = math.ceil(missing / refill_rate)
    else
        retry_after = 60
    end
end

redis.call('HSET', key, 'tokens', string.format('%.6f', tokens), 'last_refill', string.format('%.0f', last_refill))

-- Expire key after time needed to fully refill the bucket (minimum 60 seconds)
local ttl_ms = 60000
if refill_rate > 0 then
    ttl_ms = math.max(60000, math.ceil((capacity / refill_rate) * 1000))
end
redis.call('PEXPIRE', key, ttl_ms)

local remaining = math.floor(tokens)
return { allowed, remaining, retry_after }

