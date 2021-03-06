# --- !Ups
CREATE TABLE infos (
  infoId                    SERIAL PRIMARY KEY,
  displayIfConnected        BOOLEAN NOT NULL DEFAULT TRUE,
  title                     VARCHAR NOT NULL,
  content                   VARCHAR,
  animationContent          VARCHAR,
  animationStyle            VARCHAR
);


CREATE TABLE frenchCities (
  cityId                    SERIAL PRIMARY KEY,
  city                      VARCHAR(255) NOT NULL,
  geographicPoint           GEOMETRY NOT NULL
);

CREATE INDEX frenchCityGeographicPoints ON frenchCities USING GIST (geographicPoint);
CREATE INDEX frenchCityNames ON frenchCities (city);


CREATE TABLE addresses (
  addressId                 SERIAL PRIMARY KEY,
  geographicPoint           GEOMETRY DEFAULT ST_GeomFromText('POINT(-84 30)', 4326) NOT NULL,
  city                      VARCHAR(127),
  zip                       VARCHAR(15),
  street                    VARCHAR,
  country                   VARCHAR
);
CREATE INDEX geographicPointAddresses ON addresses USING GIST (geographicPoint);
CREATE UNIQUE INDEX addressesIndex ON addresses (city, zip, street, country);


CREATE TABLE orders ( --account701
  orderId                   SERIAL PRIMARY KEY,
  totalPrice                INT NOT NULL
);


CREATE TABLE artists(
  facebookId                VARCHAR(63) UNIQUE NOT NULL PRIMARY KEY,
  creationDateTime          TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  name                      VARCHAR(255) NOT NULL,
  imagePath                 VARCHAR,
  description               VARCHAR,
  facebookUrl               VARCHAR(255) UNIQUE NOT NULL,
  websites                  VARCHAR,
  hasTracks                 BOOLEAN DEFAULT FALSE,
  likes                     INTEGER,
  country                   VARCHAR
);


CREATE TABLE organizers(
  organizerId             SERIAL PRIMARY KEY,
  facebookId              VARCHAR(63) UNIQUE NOT NULL,
  facebookUrl             VARCHAR(255) UNIQUE NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  description             VARCHAR,
  addressId               BIGINT REFERENCES addresses(addressId) ON DELETE CASCADE,
  phone                   VARCHAR(255),
  publicTransit           VARCHAR,
  websites                VARCHAR,
  verified                BOOLEAN DEFAULT FALSE NOT NULL,
  imagePath               VARCHAR,
  geographicPoint         GEOMETRY DEFAULT ST_GeomFromText('POINT(-84 30)', 4326) NOT NULL,
  placeUrl                VARCHAR(255),
  likes                   INTEGER
);


CREATE TABLE genres (
  genreId                 SERIAL PRIMARY KEY,
  name                    VARCHAR NOT NULL,
  icon                    CHAR NOT NULL,
  UNIQUE(name)
);


CREATE TABLE tracks(
  trackId                 UUID PRIMARY KEY NOT NULL,
  title                   VARCHAR(255) NOT NULL,
  url                     VARCHAR NOT NULL,
  platform                CHAR NOT NULL,
  thumbnailUrl            VARCHAR NOT NULL,
  artistFacebookUrl       VARCHAR(255) REFERENCES artists(facebookUrl) ON DELETE CASCADE NOT NULL,
  artistName              VARCHAR(255) NOT NULL,
  redirectUrl             VARCHAR(255),
  confidence              DOUBLE PRECISION NOT NULL DEFAULT 0,
  ratingUp                INT NOT NULL DEFAULT 0,
  ratingDown              INT NOT NULL DEFAULT 0,
  UNIQUE(url)
);
CREATE UNIQUE INDEX artistNameAndTitle ON tracks(title, artistFacebookUrl);
CREATE INDEX artistFacebookUrl ON tracks(artistFacebookUrl);


create table users (
  userId                    UUID PRIMARY KEY,
  firstName                 VARCHAR,
  lastName                  VARCHAR,
  fullName                  VARCHAR,
  email                     VARCHAR UNIQUE,
  avatarURL                 VARCHAR
);


CREATE TABLE logininfo (
  id                       SERIAL PRIMARY KEY,
  providerId               VARCHAR NOT NULL,
  providerKey              VARCHAR NOT NULL
);


CREATE TABLE userlogininfo (
  userId                   UUID PRIMARY KEY,
  loginInfoId              BIGINT NOT NULL
);


CREATE TABLE passwordinfo (
  hasher                  VARCHAR NOT NULL,
  password                VARCHAR NOT NULL,
  salt                    VARCHAR,
  loginInfoId             BIGINT NOT NULL
);


CREATE TABLE oauth1info (
  id                      SERIAL PRIMARY KEY,
  token                   VARCHAR NOT NULL,
  secret                  VARCHAR NOT NULL,
  loginInfoId             BIGINT NOT NULL
);


CREATE TABLE oauth2info (
  id                      SERIAL PRIMARY KEY,
  accesstoken             VARCHAR NOT NULL,
  tokentype               VARCHAR,
  expiresin               INTEGER,
  refreshtoken            VARCHAR,
  logininfoid             BIGINT NOT NULL
);


CREATE TABLE openidinfo (
  id                      VARCHAR NOT NULL PRIMARY KEY,
  logininfoid             BIGINT NOT NULL
);


CREATE TABLE openidattributes (
  id                      VARCHAR NOT NULL,
  key                     VARCHAR NOT NULL,
  value                   VARCHAR NOT NULL
);

CREATE TABLE receivedMails (
  id                        SERIAL PRIMARY KEY,
  subject                   VARCHAR NOT NULL,
  message                   VARCHAR NOT NULL,
  read                      BOOLEAN NOT NULL DEFAULT FALSE,
  userId                    UUID REFERENCES users(userId) ON DELETE CASCADE
);

CREATE TABLE events(
  event_id                  SERIAL PRIMARY KEY,
  event_facebook_id         VARCHAR(63) NOT NULL,
  creationDateTime          TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  name                      VARCHAR(255) NOT NULL,
  geographicPoint           GEOMETRY DEFAULT ST_GeomFromText('POINT(-84 30)', 4326) NOT NULL,
  description               VARCHAR,
  startTime                 TIMESTAMP WITH TIME ZONE NOT NULL,
  endTime                   TIMESTAMP WITH TIME ZONE,
  imagePath                 VARCHAR,
  ageRestriction            SMALLINT NOT NULL DEFAULT 16,
  tariffRange               VARCHAR(255),
  ticketSellers             VARCHAR,
  UNIQUE(event_facebook_id)
);
CREATE INDEX eventGeographicPoint ON events USING GIST (geographicPoint);


CREATE TABLE places (
  placeId                   SERIAL PRIMARY KEY,
  name                      VARCHAR(255) NOT NULL,
  geographicPoint           GEOMETRY DEFAULT ST_GeomFromText('POINT(-84 30)', 4326) NOT NULL,
  addressId                 BIGINT REFERENCES addresses(addressId) ON DELETE CASCADE,
  facebookId                VARCHAR(63) UNIQUE NOT NULL,
  facebookUrl               VARCHAR(63) UNIQUE,
  description               VARCHAR,
  webSites                  VARCHAR,
  facebookMiniature         VARCHAR,
  capacity                  INT,
  openingHours              VARCHAR(255),
  imagePath                 VARCHAR,
  linkedOrganizerId         VARCHAR(255),
  likes                     INTEGER
);
CREATE INDEX placeGeographicPoint ON places USING GIST(geographicPoint);
CREATE UNIQUE INDEX geographicPointName ON places(name, geographicPoint);


CREATE TABLE images(
  imageId                   SERIAL PRIMARY KEY,
  path                      VARCHAR NOT NULL,
  category                  VARCHAR(31),
  organizerId               BIGINT REFERENCES organizers(organizerId) ON DELETE CASCADE,
  infoId                    BIGINT REFERENCES infos(infoId) ON DELETE CASCADE,
  trackId                   UUID REFERENCES tracks(trackId) ON DELETE CASCADE,
  UNIQUE(path)
);

CREATE TABLE tariffs(
  tariffId                  SERIAL PRIMARY KEY,
  denomination              VARCHAR(255) DEFAULT 'Basique' NOT NULL,
  price                     NUMERIC NOT NULL,
  startTime                 TIMESTAMP WITH TIME ZONE NOT NULL,
  endTime                   TIMESTAMP WITH TIME ZONE NOT NULL,
  event_id                  BIGINT REFERENCES events(event_id) ON DELETE CASCADE
);


CREATE TABLE tickets(
  ticketId                  SERIAL PRIMARY KEY,
  qrCode                    VARCHAR(255) UNIQUE NOT NULL,
  event_id                  INT REFERENCES events(event_id) NOT NULL,
  tariffId                  INT REFERENCES tariffs(tariffId) NOT NULL
);
CREATE UNIQUE INDEX ticketQrCode ON tickets (qrCode);

CREATE TABLE proposedTariffs(
  id                        SERIAL PRIMARY KEY,
  tariffId                  INT REFERENCES tariffs(tariffId) NOT NULL,
  amount                    NUMERIC NOT NULL,
  timestamp                 TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE userPropositions(
  id                        SERIAL PRIMARY KEY,
  proposedTariffId          INT REFERENCES proposedTariffs(id) ON DELETE CASCADE NOT NULL,
  tariffId                  INT REFERENCES tariffs(tariffId) ON DELETE CASCADE NOT NULL,
  userId                    UUID REFERENCES users(userId) NOT NULL,
  timestamp                 TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ticketStatuses(
  id                        SERIAL PRIMARY KEY,
  ticketId                  INT REFERENCES tickets(ticketId) NOT NULL,
  status                    CHAR NOT NULL,
  date                      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE pendingOrders(
  uuid                      UUID PRIMARY KEY
);


CREATE TABLE blockedTickets(
  id                        SERIAL PRIMARY KEY,
  ticketId                  INT REFERENCES tickets(ticketId) NOT NULL,
  expirationDate            TIMESTAMP WITH TIME ZONE NOT NULL,
  userId                    UUID REFERENCES users(userId) NOT NULL,
  tariffId                  INT REFERENCES tariffs(tariffId) NOT NULL,
  orderId                   UUID REFERENCES pendingOrders(uuid) NOT NULL
);

CREATE TABLE boughtTicketBills (
  billId                    SERIAL PRIMARY KEY,
  ticketId                  INT REFERENCES tickets(ticketId) NOT NULL,
  userId                    UUID REFERENCES users (userId) NOT NULL,
  date                      TIMESTAMP WITH TIME ZONE NOT NULL,
  amount                    NUMERIC NOT NULL,
  orderId                   UUID REFERENCES pendingOrders(uuid)
);

CREATE TABLE soldTicketBills (
  billId                    SERIAL PRIMARY KEY,
  ticketId                  INT REFERENCES tickets(ticketId) NOT NULL,
  userId                    UUID REFERENCES users (userId) NOT NULL,
  date                      TIMESTAMP WITH TIME ZONE NOT NULL,
  amount                    NUMERIC NOT NULL,
  orderId                   UUID REFERENCES pendingOrders(uuid)
);

CREATE TABLE tokenMovements(
  id                        SERIAL PRIMARY KEY,
  creationDate              TIMESTAMP WITH TIME ZONE NOT NULL,
  amount                    NUMERIC NOT NULL,
  userId                    UUID REFERENCES users(userId) NOT NULL,
  boughtTicketBillId        INT REFERENCES boughtTicketBills(billId),
  soldTicketOrderId         UUID REFERENCES pendingOrders(uuid),
  availableDate             TIMESTAMP WITH TIME ZONE,
  isWithdraw                BOOLEAN NOT NULL
);

CREATE TABLE conflicts(
  id                        SERIAL PRIMARY KEY,
  creationDate              TIMESTAMP WITH TIME ZONE NOT NULL,
  amount                    NUMERIC NOT NULL,
  sellerId                  UUID REFERENCES users(userId) NOT NULL,
  buyerId                   UUID REFERENCES users(userId) NOT NULL,
  refundedUserId            UUID REFERENCES users(userId),
  boughtTicketBillId        INT REFERENCES boughtTicketBills(billId) NOT NULL,
  soldTicketOrderId         UUID REFERENCES pendingOrders(uuid) NOT NULL,
  refundDate                TIMESTAMP WITH TIME ZONE
);

CREATE TABLE pendingTickets (
  pendingTicketId           SERIAL PRIMARY KEY,
  userId                    UUID REFERENCES users (userId) NOT NULL,
  tariffId                  INT REFERENCES tariffs(tariffId)  NOT NULL,
  date                      TIMESTAMP WITH TIME ZONE NOT NULL,
  amount                    NUMERIC NOT NULL,
  qrCode                    VARCHAR UNIQUE NOT NULL,
  event_id                  INT REFERENCES events(event_id) NOT NULL,
  isValidated               BOOLEAN
);
CREATE UNIQUE INDEX pendingTicketQrCode ON pendingTickets (qrCode);


CREATE TABLE salableEvents (
  event_facebook_id         VARCHAR PRIMARY KEY REFERENCES events(event_facebook_id) NOT NULL
);

CREATE TABLE barCodesWithUrl(
  id                        SERIAL PRIMARY KEY,
  codeType                  VARCHAR(255) NOT NULL,
  code                      VARCHAR(255) NOT NULL,
  fileName                  UUID NOT NULL,
  maybeUrl                  VARCHAR(255),
  userId                    UUID REFERENCES users(userId) NOT NULL,
  event_facebook_id         VARCHAR REFERENCES salableEvents(event_facebook_id) NOT NULL
);
CREATE UNIQUE INDEX userQrCode ON barCodesWithUrl(code, userId);

CREATE TABLE guestUsers (
  ip                        VARCHAR(32) PRIMARY KEY,
  userUuid                  UUID REFERENCES users(userId),
  email                     VARCHAR(255)
);

CREATE TABLE userSessions (
  id                        UUID PRIMARY KEY,
  ip                        VARCHAR(32) REFERENCES guestUsers(ip) NOT NULL,
  screenWidth               INT NOT NULL,
  screenHeight              INT NOT NULL,
  timestamp                 TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL
);

CREATE TABLE userActions (
  id                        SERIAL PRIMARY KEY ,
  action                    VARCHAR(255) NOT NULL,
  timestamp                 TIMESTAMP NOT NULL,
  sessionId                 UUID REFERENCES userSessions(id)
);


CREATE TABLE ribs (
  id                        SERIAL PRIMARY KEY,
  bankCode                  VARCHAR(255) NOT NULL,
  deskCode                  VARCHAR(255) NOT NULL,
  accountNumber             VARCHAR(255) NOT NULL UNIQUE,
  ribKey                    VARCHAR(255) NOT NULL,
  userId                    UUID REFERENCES users (userId) NOT NULL,
  creationTime              TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL
);

CREATE TABLE idCards (
  uuid                      UUID PRIMARY KEY,
  userId                    UUID REFERENCES users (userId) NOT NULL,
  creationTime              TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL
);


CREATE TABLE issues (
  issueId                   SERIAL PRIMARY KEY,
  title                     VARCHAR NOT NULL,
  content                   VARCHAR,
  userId                    UUID REFERENCES users (userId) ON DELETE CASCADE,
  fixed                     BOOLEAN DEFAULT FALSE NOT NULL
);


CREATE TABLE issuesComments (
  commentId                 SERIAL PRIMARY KEY,
  content                   VARCHAR,
  userId                    UUID REFERENCES users (userId) ON DELETE CASCADE,
  issueId                   BIGINT REFERENCES issues(issueId) ON DELETE CASCADE
);


CREATE TABLE usersTools (
  tableId                   SERIAL PRIMARY KEY,
  tools                     VARCHAR(255) NOT NULL,
  userId                    UUID REFERENCES users(userId) ON DELETE CASCADE
);

---############################## ACCOUNTING ###################################

---Vente de produits finis = account701 = table order

CREATE TABLE clients (
  clientId                SERIAL PRIMARY KEY,
  name                    VARCHAR(255),
  contactName             VARCHAR(255),
  socialDenomination      VARCHAR(255),
  addressID               BIGINT REFERENCES addresses(addressID) ON DELETE CASCADE,
  email                   VARCHAR(255),
  UNIQUE(email)
);

CREATE TABLE bills (
  id                      SERIAL PRIMARY KEY,
  clientId                BIGINT REFERENCES clients(clientId),
  billingDate             TIMESTAMP WITH TIME ZONE,
  amountDue               INT,
  endSellingTime          TIMESTAMP WITH TIME ZONE
  --majorations impayés
);


--Produit activités annexes = compte 708
CREATE TABLE account708 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP WITH TIME ZONE DEFAULT  current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  clientId                BIGINT NOT NULL REFERENCES clients(clientId),
  orderId                 BIGINT NOT NULL REFERENCES orders(orderId)
);

--Quand le client a payé sa facture
CREATE TABLE account411 (
  id                      SERIAL PRIMARY KEY,
  clientId                BIGINT NOT NULL REFERENCES clients(clientId),
  paymentDate             TIMESTAMP WITH TIME ZONE NOT NULL,
  amount                  INT NOT NULL,
  paymentMean             VARCHAR(255) NOT NULL
);

--Ce qui est dû par le client
CREATE TABLE account413 (
  id                      SERIAL PRIMARY KEY,
  clientId                BIGINT REFERENCES clients(clientId),
  date                    TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  amount                  INT NOT NULL,
  debit                   Boolean NOT NULL
);

--Accomptes payés par les clients
CREATE TABLE account4191 (
  id                      SERIAL PRIMARY KEY,
  clientId                BIGINT REFERENCES clients(clientId),
  billId                  BIGINT REFERENCES bills(id),
  paymentDate             TIMESTAMP WITH TIME ZONE NOT NULL,
  amount                  INT NOT NULL,
  paymentMean             VARCHAR(255) NOT NULL
);


--associés, dividendes à payer
CREATE TABLE account457 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP WITH TIME ZONE DEFAULT  current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  paymentReference        INT NOT NULL,
  assemblyName            VARCHAR(255) NOT NULL
);

--impôts et taxes
CREATE TABLE account63 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  orderId                 BIGINT REFERENCES orders(orderId),
  account708Id            BIGINT REFERENCES account708(id)
);

--diverses charges à payer
CREATE TABLE account4686 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255),
  amount                  NUMERIC NOT NULL,
  debit                   BOOLEAN NOT NULL,
  account63Id             BIGINT REFERENCES account63(id)
);


--achats
CREATE TABLE account60 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  paymentReference        INT,
  orderId                 BIGINT REFERENCES orders(orderId)
);

--Fournisseurs à payer
CREATE TABLE account403 (
  id                      SERIAL PRIMARY KEY,
  date                    TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  amount                  NUMERIC NOT NULL,
  debit                   BOOLEAN NOT NULL, --#sinon debit
  userId                  UUID REFERENCES users(userId),
  account60Id             BIGINT REFERENCES account60(id)
);

--frais postaux et telecoms
CREATE TABLE account626 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  paymentReference        INT NOT NULL
);

--services bancaires
CREATE TABLE account627 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  amount                  NUMERIC NOT NULL,
  orderId                 BIGINT REFERENCES orders(orderId)
);


CREATE TABLE bank ( --account512
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
  name                    VARCHAR(255) DEFAULT '666' NOT NULL,
  amount                  NUMERIC NOT NULL,
  debit                   Boolean NOT NULL,
  paymentReference        INT DEFAULT 0 NOT NULL,
  orderId                 BIGINT REFERENCES orders(orderId),
  account627Id            BIGINT REFERENCES account627(id)
);

--publicité publications et relations publiques
CREATE TABLE account623 (
  id                      SERIAL PRIMARY KEY,
  datePayment             TIMESTAMP WITH TIME ZONE DEFAULT  current_timestamp NOT NULL,
  amount                  NUMERIC NOT NULL,
  name                    VARCHAR(255) NOT NULL,
  paymentReference        INT NOT NULL,
  billId                  BIGINT REFERENCES bills(id)
);

CREATE TABLE followedEvents (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId) ON DELETE CASCADE,
  guestUserIP              VARCHAR REFERENCES guestUsers(ip) ON DELETE CASCADE,
  event_id                 BIGINT REFERENCES events(event_id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX followedEventsByUserIdIndex ON followedEvents (userId, event_id);
CREATE UNIQUE INDEX followedEventsByGuestUserIndex ON followedEvents (guestUserIP, event_id);

CREATE TABLE artistsFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId) ON DELETE CASCADE,
  artistId                 VARCHAR REFERENCES artists(facebookId) ON DELETE CASCADE
);
CREATE UNIQUE INDEX artistsFollowedIndex ON artistsFollowed (userId, artistId);


CREATE TABLE placesFollowed (
  tableId                  SERIAL PRIMARY KEY,
  userId                   UUID REFERENCES users(userId) ON DELETE CASCADE,
  placeFacebookUrl         VARCHAR(255) REFERENCES places(facebookUrl) ON DELETE CASCADE NOT NULL
);
CREATE UNIQUE INDEX placesFollowedIndex ON placesFollowed (userId, placeFacebookUrl);

CREATE TABLE usersFollowed (
  tableId                 SERIAL PRIMARY KEY,
  userIdFollower          UUID REFERENCES users(userId),
  userIdFollowed          UUID REFERENCES users(userId)
);
CREATE UNIQUE INDEX usersFollowedIndex ON usersFollowed (userIdFollower, userIdFollowed);

CREATE TABLE organizersFollowed (
  userId                  UUID REFERENCES users(userId) ON DELETE CASCADE,
  organizerUrl            VARCHAR REFERENCES organizers(facebookUrl) ON DELETE CASCADE NOT NULL,
  PRIMARY KEY (userId, organizerUrl)
);

CREATE TABLE tracksFollowed (
  userId                   UUID REFERENCES users(userId) ON DELETE CASCADE,
  trackId                  UUID REFERENCES tracks(trackId) ON DELETE CASCADE,
  PRIMARY KEY (userId, trackId)
);

CREATE TABLE eventsPlaces (
  event_id                VARCHAR REFERENCES events(event_facebook_id) ON DELETE CASCADE,
  placeFacebookUrl        VARCHAR REFERENCES places(facebookUrl) ON DELETE CASCADE,
  PRIMARY KEY (event_id, placeFacebookUrl)
);


CREATE TABLE eventsGenres (
  event_id                VARCHAR REFERENCES events(event_facebook_id) ON DELETE CASCADE,
  genreId                 INT REFERENCES genres(genreId) ON DELETE CASCADE,
  PRIMARY KEY (event_id, genreId)
);


CREATE TABLE eventsOrganizers (
  event_id                VARCHAR REFERENCES events(event_facebook_id) ON DELETE CASCADE,
  organizerUrl            VARCHAR REFERENCES organizers(facebookUrl) ON DELETE CASCADE,
  PRIMARY KEY (event_id, organizerUrl)
);


CREATE TABLE eventsAddresses (
  event_id                VARCHAR REFERENCES events(event_facebook_id) ON DELETE CASCADE,
  addressId               INT REFERENCES addresses(addressId) ON DELETE CASCADE,
  PRIMARY KEY (event_id, addressId)
);


CREATE TABLE usersOrganizers (
  userId                  UUID REFERENCES users(userId) ON DELETE CASCADE,
  organizerId             INT REFERENCES organizers(organizerId) ON DELETE CASCADE,
  PRIMARY KEY (userId, organizerId)
);

CREATE TABLE eventsArtists (
  event_id                VARCHAR REFERENCES events(event_facebook_id) ON DELETE CASCADE,
  artistId                VARCHAR REFERENCES artists(facebookId) ON DELETE CASCADE,
  PRIMARY KEY (event_id, artistId)
);


CREATE TABLE tracksGenres (
  trackId                 UUID REFERENCES tracks(trackId) ON DELETE CASCADE NOT NULL,
  genreId                 INT REFERENCES genres(genreId) ON DELETE CASCADE NOT NULL,
  weight                  BIGINT NOT NULL,
  PRIMARY KEY(trackId, genreId)
);


CREATE TABLE artistsGenres (
  artistId                VARCHAR REFERENCES artists(facebookId) ON DELETE CASCADE,
  genreId                 INT REFERENCES genres(genreId) ON DELETE CASCADE,
  weight                  INT NOT NULL,
  PRIMARY KEY (artistId, genreId)
);


CREATE TABLE playlists (
  playlistId              SERIAL PRIMARY KEY,
  userId                  UUID REFERENCES users(userId) ON DELETE CASCADE NOT NULL,
  name                    VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX playlistsIndex ON playlists (playlistId, userId);


CREATE TABLE playlistsTracks (
  playlistId              BIGINT REFERENCES playlists (playlistId) ON DELETE CASCADE,
  trackId                 UUID REFERENCES tracks (trackId) ON DELETE CASCADE,
  trackRank               DOUBLE PRECISION NOT NULL,
  PRIMARY KEY (playlistId, trackId)
);

CREATE TABLE tracksRating (
  userId                  UUID REFERENCES users(userId) ON DELETE CASCADE NOT NULL,
  trackId                 UUID REFERENCES tracks(trackId) ON DELETE CASCADE NOT NULL,
  ratingUp                INT,
  ratingDown              INT,
  reason                  CHAR,
  PRIMARY KEY(userId, trackId)
);


CREATE TABLE facebookAttendees (
  attendeeFacebookId      VARCHAR(255) PRIMARY KEY NOT NULL UNIQUE,
  name                    VARCHAR(255) NOT NULL
);


CREATE TABLE facebookAttendeeEventRelations (
  attendeeFacebookId      VARCHAR REFERENCES facebookAttendees(attendeeFacebookId) NOT NULL,
  eventFacebookId         VARCHAR(63) REFERENCES events(event_facebook_id) NOT NULL,
  attendeeStatus          CHAR,
  PRIMARY KEY (attendeeFacebookId, eventFacebookId)
);

CREATE TABLE eventscounts(
  event_facebook_id     VARCHAR PRIMARY KEY REFERENCES events(event_facebook_id) ON DELETE CASCADE NOT NULL,
  attending_count       INTEGER,
  declined_count        INTEGER,
  interested_count      INTEGER,
  maybe_count           INTEGER,
  noreply_count         INTEGER
);

# --- !Downs
DROP TABLE IF EXISTS tracksFollowed, tracksRating;
DROP TABLE IF EXISTS usersPlaylists;
DROP TABLE IF EXISTS playlistsTracks;
DROP TABLE IF EXISTS eventsGenres;
DROP TABLE IF EXISTS eventsPlaces;
DROP TABLE IF EXISTS eventsOrganizers;
DROP TABLE IF EXISTS eventsAddresses;
DROP TABLE IF EXISTS usersOrganizers;
DROP TABLE IF EXISTS eventsArtists;
DROP TABLE IF EXISTS artistsGenres;
DROP TABLE IF EXISTS tracksGenres;
DROP TABLE IF EXISTS followedEvents, facebookAttendeeEventRelations;
DROP TABLE IF EXISTS artistsFollowed;
DROP TABLE IF EXISTS placesFollowed;
DROP TABLE IF EXISTS usersFollowed;
DROP TABLE IF EXISTS organizersFollowed;
DROP TABLE IF EXISTS usersTools;
DROP TABLE IF EXISTS ticketStatuses;
DROP TABLE IF EXISTS blockedTickets;
DROP TABLE IF EXISTS boughtTicketBills CASCADE;
DROP TABLE IF EXISTS soldTicketBills CASCADE;
DROP TABLE IF EXISTS tokenMovements CASCADE;
DROP TABLE IF EXISTS conflicts CASCADE;
DROP TABLE IF EXISTS pendingOrders;
DROP TABLE IF EXISTS pendingTickets;
DROP TABLE IF EXISTS barCodesWithUrl;
DROP TABLE IF EXISTS salableEvents;
DROP TABLE IF EXISTS userActions;
DROP TABLE IF EXISTS userSessions;
DROP TABLE IF EXISTS guestUsers;
DROP TABLE IF EXISTS idCards;
DROP TABLE IF EXISTS ribs;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS proposedTariffs CASCADE;
DROP TABLE IF EXISTS userPropositions;
DROP TABLE IF EXISTS eventscounts;
DROP TABLE IF EXISTS tariffs;
DROP TABLE IF EXISTS bank;
DROP TABLE IF EXISTS account411;
DROP TABLE IF EXISTS account413;
DROP TABLE IF EXISTS account4191;
DROP TABLE IF EXISTS account403;
DROP TABLE IF EXISTS account457;
DROP TABLE IF EXISTS account60;
DROP TABLE IF EXISTS account626;
DROP TABLE IF EXISTS account627;
DROP TABLE IF EXISTS account623;
DROP TABLE IF EXISTS account4686;
DROP TABLE IF EXISTS account63;
DROP TABLE IF EXISTS account708;
DROP TABLE IF EXISTS issuesComments;
DROP TABLE IF EXISTS issues;
DROP TABLE IF EXISTS receivedMails;
DROP TABLE IF EXISTS usersTools;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS playlists;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS infos;
DROP TABLE IF EXISTS places;
DROP TABLE IF EXISTS amountDue;
DROP TABLE IF EXISTS genres;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS tracks;
DROP TABLE IF EXISTS organizers;
DROP TABLE IF EXISTS users_token;
DROP TABLE IF EXISTS artists;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS frenchCities;
DROP TABLE IF EXISTS users, logininfo, userlogininfo, passwordinfo, oauth1info,  oauth2info, openidinfo, openidattributes;
DROP TABLE IF EXISTS facebookAttendees;
