export default function Classification({value,className=''}){let f=value==='FALSO';return <span className={`classification ${f?'false':'credible'} ${className}`}><i/>{f?'Falso':'Credível'}</span>}
