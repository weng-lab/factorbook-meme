FROM openjdk:8-jre-slim as base
RUN apt-get update && apt-get install -y \
    build-essential \
    wget \
    zlib1g-dev \
    perl \
    libexpat1 \
    libxml-parser-perl \
    ghostscript && \
    wget http://hgdownload.cse.ucsc.edu/admin/exe/linux.x86_64/twoBitToFa -P /bin && \
    chmod 755 /bin/twoBitToFa && \
    wget http://meme-suite.org/meme-software/5.0.4/meme-5.0.4.tar.gz && tar zxf meme-5.0.4.tar.gz && \
    cd meme-5.0.4 && ./configure --enable-build-libxml2 --enable-build-libxslt && \
    make && make install && cp /root/bin/* /bin && cp /root/libexec/meme-5.0.4/* /bin && \
    cd .. && rm -rf meme-5.0.4 && rm meme-5.0.4.tar.gz && \
    apt-get purge --auto-remove -y build-essential wget && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

FROM openjdk:8-jdk-alpine as build
COPY . /src
WORKDIR /src
RUN ./gradlew clean shadowJar

FROM base
COPY --from=build /src/build/factorbook-meme-*.jar /app/meme.jar