package io.prediction.commons.appdata.mongodb

import io.prediction.commons.MongoUtils._
import io.prediction.commons.appdata.{Item, Items}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._
import com.github.nscala_time.time.Imports._

/** MongoDB implementation of Items. */
class MongoItems(db: MongoDB) extends Items {
  private val itemColl = db("items")

  /** Indices and hints. */
  val starttimeIndex = MongoDBObject("starttime" -> -1)
  itemColl.ensureIndex(starttimeIndex)

  RegisterJodaTimeConversionHelpers()

  def insert(item: Item) = {
    val id = MongoDBObject("_id" -> idWithAppid(item.appid, item.id))
    val appid = MongoDBObject("appid" -> item.appid)
    val ct = MongoDBObject("ct" -> item.ct)
    val itypes = MongoDBObject("itypes" -> item.itypes)
    val starttime = item.starttime map { s => MongoDBObject("starttime" -> s) } getOrElse emptyObj
    val endtime = item.endtime map { e => MongoDBObject("endtime" -> e) } getOrElse emptyObj
    val price = item.price map { p => MongoDBObject("price" -> p) } getOrElse emptyObj
    val profit = item.profit map { p => MongoDBObject("profit" -> p) } getOrElse emptyObj
    val lnglat = item.latlng map { l => MongoDBObject("lnglat" -> MongoDBList(l._2, l._1)) } getOrElse emptyObj
    val inactive = item.inactive map { i => MongoDBObject("inactive" -> i) } getOrElse emptyObj
    val attributes = item.attributes map { a => MongoDBObject("attributes" -> a) } getOrElse emptyObj
    itemColl.insert(id ++ appid ++ ct ++ itypes ++ starttime ++ endtime ++ price ++ profit ++ lnglat ++ inactive ++ attributes)
  }

  def get(appid: Int, id: String) = {
    itemColl.findOne(MongoDBObject("_id" -> idWithAppid(appid, id))) map { dbObjToItem(_) }
  }

  def getRecentByIds(appid: Int, ids: List[String]) = {
    itemColl.find(MongoDBObject("_id" -> MongoDBObject("$in" -> ids.map(idWithAppid(appid, _))))).sort(starttimeIndex).toList map { dbObjToItem(_) }
  }

  def update(item: Item) = {
    val id = MongoDBObject("_id" -> idWithAppid(item.appid, item.id))
    val appid = MongoDBObject("appid" -> item.appid)
    val ct = MongoDBObject("ct" -> item.ct)
    val itypes = MongoDBObject("itypes" -> item.itypes)
    val starttime = item.starttime map { s => MongoDBObject("starttime" -> s) } getOrElse emptyObj
    val endtime = item.endtime map { e => MongoDBObject("endtime" -> e) } getOrElse emptyObj
    val price = item.price map { p => MongoDBObject("price" -> p) } getOrElse emptyObj
    val profit = item.profit map { p => MongoDBObject("profit" -> p) } getOrElse emptyObj
    val lnglat = item.latlng map { l => MongoDBObject("lnglat" -> MongoDBList(l._2, l._1)) } getOrElse emptyObj
    val inactive = item.inactive map { i => MongoDBObject("inactive" -> i) } getOrElse emptyObj
    val attributes = item.attributes map { a => MongoDBObject("attributes" -> a) } getOrElse emptyObj
    itemColl.update(id, id ++ appid ++ ct ++ itypes ++ starttime ++ endtime ++ price ++ profit ++ lnglat ++ inactive ++ attributes)
  }

  def delete(appid: Int, id: String) = itemColl.remove(MongoDBObject("_id" -> idWithAppid(appid, id)))
  def delete(item: Item) = delete(item.appid, item.id)

  def deleteByAppid(appid: Int): Unit = {
    itemColl.remove(MongoDBObject("appid" -> appid))
  }

  private def dbObjToItem(dbObj: DBObject) = {
    val appid = dbObj.as[Int]("appid")
    Item(
      id         = dbObj.as[String]("_id").drop(appid.toString.length + 1),
      appid      = appid,
      ct         = dbObj.as[DateTime]("ct"),
      itypes     = mongoDbListToListOfString(dbObj.as[MongoDBList]("itypes")),
      starttime  = dbObj.getAs[DateTime]("starttime"),
      endtime    = dbObj.getAs[DateTime]("endtime"),
      price      = dbObj.getAs[Double]("price"),
      profit     = dbObj.getAs[Double]("profit"),
      latlng     = dbObj.getAs[MongoDBList]("lnglat") map { lnglat => (lnglat(1).asInstanceOf[Double], lnglat(0).asInstanceOf[Double]) },
      inactive   = dbObj.getAs[Boolean]("inactive"),
      attributes = dbObj.getAs[DBObject]("attributes") map { dbObjToMap(_) }
    )
  }
}