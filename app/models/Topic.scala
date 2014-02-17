package models

import org.squeryl.KeyedEntity

/**
 * Created by claudemamo on 06/02/2014.
 */
case class Topic(val name: String) extends KeyedEntity[Long] {

  override val id = 0L

}
