local stockKey = KEYS[1]
local orderKey = KEYS[2]
local userId = ARGV[1]

--local Redis = require('redis')
---- import redis
--local redis = Redis.connect('127.0.0.1', 6379)
--redis:auth('124356')
---- redis:call('auth', )

--local stockKey = "seckill:stock:11"
--local orderKey = "seckill:order:11"
--local userId = 1012


if(tonumber(redis.call('get', stockKey)) < 1)then
    return 1
end

---- 第一个下单的用户，redis中没有他的数据
--if (tonumber(redis.call('EXISTS', orderKey)) > 0) then
--     redis.call('incrby', stockKey, -1)
--     redis.call('sadd', orderKey, userId)
--     return 0
--end
-- 用户是否已下单，判断给定key是否在set中
 if (tonumber(redis.call('sismember', orderKey, userId)) > 0) then
     return 2
 end

 -- 扣减库存后存入set中
 redis.call('incrby', stockKey, -1)
 redis.call('sadd', orderKey, userId)
 return 0
