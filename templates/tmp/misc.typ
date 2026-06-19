#let data = json("/data/tmp/misc.json")

#show terms.item: it => block(width: 100%)[
  #text(style: "italic")[#it.term]
  #h(0.5em)
  #it.description
]

#set document(title: data.at("tittel", default: "ingen tittel"))
#set page(
    paper: "a4",
    margin: (top: 2.5cm, bottom: 2.5cm, left: 2.5cm, right: 2.5cm),
    header: context [
        #data.at("tittel")
        #h(1fr)
        Side  #counter(page).display("1/1", both:true)
    ],
    background: [
        #place(dy: 20mm, line(length: 100%))
    ]
)
#set text(font: "Source Sans 3", lang: "nb", size: 11pt)

#let render(elem) = [
    #if elem.at("type") == "avsnitt" [
        #elem.at("avsnitt")
        #parbreak()
    ] else if elem.at("type") == "overskrift" [
        #heading(
            level: elem.at("nivå"),
            numbering: "1.1 ",
            elem.at("overskrift"))
    ] else if elem.at("type") == "liste" [
        #for (key, value) in elem.at("liste") [
            / #key: #value
        ]
    ] else if elem.at("type") == "tabell" [
        #table(
            columns: elem.at("kolonner").len(),
            align: right,
            table.header(..elem.at("kolonner")),
            ..elem.at("rader").flatten(),
            table.footer(..elem.at("kolonner")),
         )
    ]
]

#for elem in data.at("body") [
    #render(elem)
]
