package ch.epfl.bigdata.btc.crawler.coins.indicators

import akka.actor.ActorRef
import ch.epfl.bigdata.btc.types.Registration._
import ch.epfl.bigdata.btc.types.Transfer._

class WalletSMA(dataSource: ActorRef, watched: SMARegistration)
    extends Wallet[SMARegistration] (dataSource, watched, watched.market, watched.c) {

  var values : List[Double] = Nil 

 
  val maxBtc = 0.2
  
def receiveOther(a: Any, ar: ActorRef) {
  a match{
    case b : Points => this.values= b.values.map(_._1)
    case _ =>
  }
  }
  
   def gainUpdate() : Double = {
     
  if(this.values.length < 20){
    return 0.0
    println("WALLETSMA : length too small ")
  }
  if(this.values.length > 300){
     values = values.drop(values.length - 300).take(300)
   }  
  
  println("WALLETSMA : values ", values)
   var gain = 0.0
   val signal = tradeSignalEnv(0.025) 
   var diff_bt = 0.0
   var diff_money = 0.0
    println("WALLETSMA : signal ", signal)
    val x = trans(signal, actualPrice, maxBtc)
    diff_money = x._1 
    diff_bt = x._2 
   if (signal == 1){
 
     if(actualSentiment < oldSentiment ){
       diff_bt = diff_bt/4.0
     }
     diff_money = (-1.0)* diff_bt *actualPrice
     while (diff_money > money){
         diff_money = money/2
     }
        
     diff_bt = (-1.0)* diff_money/actualPrice
     
   }
   else if(signal == -1){
     if(actualSentiment > oldSentiment){
       diff_bt = diff_bt/4.0
     }
     while (diff_bt > btcnumber){
         diff_bt = btcnumber/2
     }
      diff_money = (-1.0)* diff_bt *actualPrice
   }

   money = money + diff_money - (Math.abs(diff_money)*0.02)
   btcnumber = btcnumber + diff_bt
   
   gain = (money - msu) + (btcnumber - btcsu)*actualPrice
   println("GAINNNNNNNNNNN, SMA" , gain) 
    
    oldSentiment = actualSentiment
    actualSentiment =0.0
    
    return gain
  }
  
  def updateTweet(t: Tweet) {
    actualSentiment += t.sentiment
  }
  def updatePrice(t : Transaction){
    oldPrice = actualPrice
    actualPrice = t.unitPrice
    
  }
  
    def tradeSignalEnv(percent: Double): Int = {
     
    val envA = values.map(_ * (1 + percent))
    val envB = values.map(_ * (1 - percent))

    println("WALLETSMA Enva", envA.last)
    println("WALLETSMA Enva", envA.take(envA.length - 2).last)
    println("WALLETSMA Enva", envB.last)
    println("WALLETSMA Enva", envB.take(envA.length - 2).last)
    println("WALLETSMA values", actualPrice)
    println("WALLETSMA values", oldPrice)
 
    
    if (envA.last < actualPrice && envA.take(envA.length - 2).last >= oldPrice) {
      1
    } else if (envB.last >= actualPrice && envB.take(envB.length - 2).last < oldPrice) {
      -1
    } else
      0
  }
   def trans(signal: Int, actualPrice: Double, maxBTC : Double): (Double, Double) = {

    val min = values.min
    val max = values.max
    
    var btc=0.0
    var money = 0.0

    if (signal == 1) {

      
      btc = (1 - (min) / (max) / 100) * maxBTC
      money = (-1)* actualPrice* btc
     
      /*
      btc = 2.0
      money = (-1.0) *2.0 * actualPrice
      * 
      */
      
    } else if (signal == -1 ) {

      btc = (-1)*(1 - (min) / (max) / 100) * maxBTC
      money = (-1)*actualPrice*btc

      /*
       btc = (-2.0)
       money = (-2.0) * actualPrice
       * 
       */
    }
    (money, btc)
  }
}