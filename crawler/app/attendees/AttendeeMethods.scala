package attendees

import scala.language.postfixOps

final case class Counts(eventFacebookId: String,
                        attending_count: Long,
                        declined_count : Long,
                        interested_count: Long,
                        maybe_count: Long,
                        noreply_count: Long)
