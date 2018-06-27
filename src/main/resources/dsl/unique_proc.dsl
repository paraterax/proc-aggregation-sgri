### 可变参数 无
  "aggs": {
    "aggs": {
      "terms": {"field": "hostname.raw", "size": 0},
      "aggs": {
        "aggs": {
          "terms": {"field": "pid", "size": 0},
          "aggs": {
            "aggs": {
              "terms": {"field": "start_time_unix", "size": 0},
              "aggs": {
                "top_hits_asc": {
                  "top_hits": {
                    "size": 1,
                    "_source": ["ppid", "user_name", "comm", "cmdline", "timestamp", "str_start_time", "str_time"],
                    "sort": {"timestamp": {"order": "asc"}}
                  }
                },
                "top_hits_desc": {
                  "top_hits": {
                    "size": 1,
                    "_source": ["timestamp", "str_time"],
                    "sort": {"timestamp": {"order": "desc"}}
                  }
                },
                "avg_pcpu": {"avg": {"field": "pcpu"}},
                "avg_pmem": {"avg": {"field": "pmem"}}
              }
            }
          }
        }
      }
    }
  }
}