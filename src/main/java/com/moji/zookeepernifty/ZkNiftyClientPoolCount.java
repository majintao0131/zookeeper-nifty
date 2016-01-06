package com.moji.zookeepernifty;
public class ZkNiftyClientPoolCount {
		private Integer maxTotal;
		private Integer maxIdle;
		private Integer	minIdle;

		public ZkNiftyClientPoolCount() {
			
		}
		public ZkNiftyClientPoolCount(Integer maxTotal, Integer maxIdle, Integer minIdle) {
			this.maxTotal = maxTotal;
			this.maxIdle = maxIdle;
			this.minIdle = minIdle;
		}
		public Integer getMaxTotal() {
			return maxTotal;
		}
		public void setMaxTotal(Integer maxTotal) {
			this.maxTotal = maxTotal;
		}
		public Integer getMaxIdle() {
			return maxIdle;
		}
		public void setMaxIdle(Integer maxIdle) {
			this.maxIdle = maxIdle;
		}
		public Integer getMinIdle() {
			return minIdle;
		}
		public void setMinIdle(Integer minIdle) {
			this.minIdle = minIdle;
		}
	}