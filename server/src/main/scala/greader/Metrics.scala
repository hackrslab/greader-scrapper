package greader

import gmetrics.GMetric._

object Metrics {
  implicit val gmetricServer = GMetricServer(name = "greader", hostname = "localhost", port = 8991)

  /* All Operations */
  val _readOps = Counter("greader.readOps")
  val _readRate = Meter("greader.readRate")

  val _writeOps = Counter("greader.writeOps")
  val _writeRate = Meter("greader.writeRate")

  /* Remote HTTP Call Latency */
  val _remoteLatency = Histogram("greader.remoteLatency")
  /* Local Cache Call Latency */
  val _localLatency = Histogram("greader.localLatency")

  /* Remote Hit */
  val _remoteHitOps = Counter("greader.remoteHitOps")
  val _remoteHitRate = Meter("greader.remoteHitRate")

  /* Cache Hit */
  val _cacheHitOps = Counter("greader.cacheHitOps")
  val _cacheHitRate = Meter("greader.cacheHitRate")

  /* Cache Miss */
  val _cacheMissOps = Counter("greader.cacheMissOps")
  val _cacheMissRate = Meter("greader.cacheMissRate")

  /* Cache Renew */
  val _cacheRenewOps = Counter("greader.cacheRenewOps")
  val _cacheRenewRate = Meter("greader.cacheRenewRate")

  /* Total Cache Access Time */
  val _cacheTime = Histogram("greader.cacheTime")

  /* Subscribe API call */
  val _subscribeOps = Counter("greader.subscribeOps")
  val _subscribeRate = Meter("greader.subscribeRate")

  val _refreshOps = Counter("greader.refreshOps")
  val _refreshRate = Meter("greader.refreshRate")

  /* Feed's item Update */
  val _itemsChanged = Counter("greader.itemsChanged")
  /* Feed's item up to date */
  val _itemsUpToDate = Counter("greader.itemsUpToDate")
  /* Fetch Error */
  val _fetchErrors = Counter("greader.fetchErrors")

  def readOp() {
    _readOps.inc()
    _readRate.mark()
  }

  def writeOp() {
    _writeOps.inc()
    _writeRate.mark()
  }

  def remoteLatency(time: Long) {
    _remoteLatency.update(time)
  }

  def localLatency(time: Long) {
    _localLatency.update(time)
  }

  def remoteHit() {
    _remoteHitOps.inc()
    _remoteHitRate.mark()
  }

  def cacheHit() {
    _cacheHitOps.inc()
    _cacheHitRate.mark()
  }

  def cacheMiss() {
    _cacheMissOps.inc()
    _cacheMissRate.mark()
  }

  def cacheRenew() {
    _cacheRenewOps.inc()
    _cacheRenewRate.mark()
  }

  def cacheTime(time: Long) {
    _cacheTime.update(time)
  }

  def subscribeOp() {
    _subscribeOps.inc()
    _subscribeRate.mark()
  }

  def itemsChanged() {
    _itemsChanged.inc()
  }

  def itemsUpToDate() {
    _itemsUpToDate.inc()
  }

  def fetchError() {
    _fetchErrors.inc()
  }



  def refreshOp() {
    _refreshOps.inc()
    _refreshRate.mark()
  }

  val _readStatusOps = Counter("greader.readStatusOps")
  val _readStatusRate = Meter("greader.readStatusRate")

  def readStatusOp() {
    _readStatusOps.inc()
    _readStatusRate.mark()
  }
}
